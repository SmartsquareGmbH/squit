package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.config.databaseConfigurations
import de.smartsquare.squit.config.endpoint
import de.smartsquare.squit.config.headers
import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.config.method
import de.smartsquare.squit.config.postRunnerScripts
import de.smartsquare.squit.config.postRunners
import de.smartsquare.squit.config.postTestTasks
import de.smartsquare.squit.config.preRunnerScripts
import de.smartsquare.squit.config.preRunners
import de.smartsquare.squit.config.preTestTasks
import de.smartsquare.squit.db.ConnectionCollection
import de.smartsquare.squit.db.executeScript
import de.smartsquare.squit.entity.SquitDatabaseConfiguration
import de.smartsquare.squit.entity.SquitMetaInfo
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.interfaces.SquitPostRunner
import de.smartsquare.squit.interfaces.SquitPreRunner
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE_INFO
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.dir
import de.smartsquare.squit.util.lifecycleOnSameLine
import de.smartsquare.squit.util.newLineIfNeeded
import groovy.lang.Binding
import groovy.lang.GroovyShell
import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.http.HttpMethod
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Driver
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Task for running requests against the given api. Also capable of running existing sql scripts before and after the
 * request.
 */
abstract class SquitRequestTask : DefaultTask() {

    /**
     * The jdbc driver classes to use.
     */
    @get:Input
    abstract val jdbcDrivers: ListProperty<String>

    /**
     * The timeout in seconds to use for requests.
     */
    @get:Input
    abstract val requestTimeout: Property<Long>

    /**
     * If squit should avoid printing anything if all tests pass.
     */
    @get:Input
    abstract val silent: Property<Boolean>

    /**
     * The class name of the jdbc [Driver] to use.
     */
    @get:Input
    val jdbcDriverClassNames: Provider<List<String>>
        get() = jdbcDrivers.map { driver -> driver.map(String::trim).filter(String::isNotBlank) }

    /**
     * The directory of the test sources.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val processedSourcesDir: Provider<Directory> = project.layout.buildDirectory
        .dir(SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The directory to save the results in.
     */
    @get:OutputDirectory
    val actualResponsesDir: Provider<Directory> = project.layout.buildDirectory
        .dir(SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(requestTimeout.get(), TimeUnit.SECONDS)
            .writeTimeout(requestTimeout.get(), TimeUnit.SECONDS)
            .readTimeout(requestTimeout.get(), TimeUnit.SECONDS)
            .build()
    }

    private val dbConnections = ConnectionCollection()

    private val preRunnersCache = mutableMapOf<String, SquitPreRunner>()
    private val postRunnersCache = mutableMapOf<String, SquitPostRunner>()

    init {
        group = "Build"
        description = "Performs the integration tests specified in the test source directory."
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        jdbcDriverClassNames.get()
            .onEach { logger.info("Using $it for jdbc connections.") }
            .forEach { Class.forName(it) }

        FilesUtils.deleteRecursivelyIfExisting(actualResponsesDir.asPath)
        Files.createDirectories(actualResponsesDir.asPath)

        dbConnections.use {
            FilesUtils.getLeafDirectories(processedSourcesDir.asPath).forEachIndexed { index, testDirectoryPath ->
                if (!silent.get()) {
                    logger.lifecycleOnSameLine(
                        "Running test ${index + 1}",
                        project.gradle.startParameter.consoleOutput,
                    )
                }

                val resultResponsePath = Files.createDirectories(
                    actualResponsesDir.asPath.resolve(testDirectoryPath.cut(processedSourcesDir.asPath)),
                )

                val errorFile = testDirectoryPath.resolve(ERROR)
                val metaFilePath = resultResponsePath.resolve(META)

                val startTime = System.currentTimeMillis()

                if (Files.exists(errorFile)) {
                    Files.copy(errorFile, resultResponsePath.resolve(ERROR))
                } else {
                    val configPath = FilesUtils.validateExistence(testDirectoryPath.resolve(CONFIG))
                    val config = ConfigFactory.parseFile(configPath.toFile())

                    val requestPath = resolveRequestPath(config, testDirectoryPath)

                    doRequestAndScriptExecutions(testDirectoryPath, resultResponsePath, requestPath, config)
                }

                val endTime = System.currentTimeMillis()
                val metaInfo = SquitMetaInfo(LocalDateTime.now(), endTime - startTime)

                Files.write(metaFilePath, metaInfo.toJson().toByteArray())
            }
        }

        logger.newLineIfNeeded()
    }

    private fun resolveRequestPath(config: Config, testPath: Path) = testPath
        .resolve(MediaTypeFactory.request(config.mediaType))
        .let {
            when {
                HttpMethod.requiresRequestBody(config.method) -> FilesUtils.validateExistence(it)
                HttpMethod.permitsRequestBody(config.method) -> when (Files.exists(it)) {
                    true -> it
                    else -> null
                }

                else -> null
            }
        }

    private fun doRequestAndScriptExecutions(
        testDirectoryPath: Path,
        resultResponsePath: Path,
        requestPath: Path?,
        config: Config,
    ) {
        val resultResponseFilePath = resultResponsePath.resolve(MediaTypeFactory.actualResponse(config.mediaType))

        doPreScriptExecutions(config, testDirectoryPath)

        try {
            val apiResponse = constructApiCall(requestPath, config).execute()
            val apiBody = apiResponse.body?.string() ?: throw IOException("Subject did not answer with a body")
            val mediaType = apiResponse.body?.contentType()

            Files.write(resultResponseFilePath, apiBody.toByteArray())

            val responseInfo = SquitResponseInfo(apiResponse.code)
            val resultResponseInfoFilePath = resultResponsePath.resolve(ACTUAL_RESPONSE_INFO)

            Files.write(resultResponseInfoFilePath, responseInfo.toJson().toByteArray())

            if (!apiResponse.isSuccessful) {
                if (!silent.get()) logger.newLineIfNeeded()

                logger.info(
                    "Unsuccessful request for test ${testDirectoryPath.cut(processedSourcesDir.asPath)} " +
                        "(status code: ${apiResponse.code})",
                )
            } else if (
                mediaType?.type != config.mediaType.type ||
                mediaType.subtype != config.mediaType.subtype
            ) {
                if (!silent.get()) logger.newLineIfNeeded()

                logger.info(
                    "Unexpected Media type $mediaType for test " +
                        "${testDirectoryPath.cut(processedSourcesDir.asPath)}. Expected ${config.mediaType}",
                )
            }
        } catch (error: IOException) {
            Files.write(resultResponsePath.resolve(ERROR), error.toString().toByteArray())
        }

        doPostScriptExecutions(config, testDirectoryPath)
    }

    private fun doPreScriptExecutions(config: Config, testDirectoryPath: Path) {
        config.preTestTasks.forEach { task ->
            when (task) {
                SquitPreTestTask.PRE_RUNNERS -> executePreRunners(config)
                SquitPreTestTask.PRE_RUNNER_SCRIPTS -> executePreRunnerScripts(config)
                SquitPreTestTask.DATABASE_SCRIPTS -> executePreDatabaseScripts(config, testDirectoryPath)
            }
        }
    }

    private fun executePreDatabaseScripts(config: Config, testDirectoryPath: Path) {
        config.databaseConfigurations.forEach {
            executeScriptIfExisting(it, testDirectoryPath.resolve("${it.name}_pre.sql"))
        }
    }

    private fun executePreRunnerScripts(config: Config) {
        config.preRunnerScripts.forEach {
            GroovyShell(javaClass.classLoader)
                .parse(it.toFile())
                .apply { binding = Binding(mapOf("config" to config)) }
                .run()
        }
    }

    private fun executePreRunners(config: Config) {
        config
            .preRunners
            .map {
                preRunnersCache.getOrPut(it) { Class.forName(it).getConstructor().newInstance() as SquitPreRunner }
            }
            .forEach { it.run(config) }
    }

    private fun doPostScriptExecutions(config: Config, testDirectoryPath: Path) {
        config.postTestTasks.forEach { task ->
            when (task) {
                SquitPostTestTask.POST_RUNNER_SCRIPTS -> executePostRunnerScripts(config)
                SquitPostTestTask.POST_RUNNERS -> executePostRunners(config)
                SquitPostTestTask.DATABASE_SCRIPTS -> executePostDatabaseScripts(config, testDirectoryPath)
            }
        }
    }

    private fun executePostDatabaseScripts(config: Config, testDirectoryPath: Path) {
        config.databaseConfigurations.forEach {
            executeScriptIfExisting(it, testDirectoryPath.resolve("${it.name}_post.sql"))
        }
    }

    private fun executePostRunnerScripts(config: Config) {
        config.postRunnerScripts.forEach {
            GroovyShell(javaClass.classLoader)
                .parse(it.toFile())
                .apply { binding = Binding(mapOf("config" to config)) }
                .run()
        }
    }

    private fun executePostRunners(config: Config) {
        config
            .postRunners
            .map {
                postRunnersCache.getOrPut(it) { Class.forName(it).getConstructor().newInstance() as SquitPostRunner }
            }
            .forEach { it.run(config) }
    }

    private fun constructApiCall(requestPath: Path?, config: Config): Call {
        val requestBody = requestPath?.toFile()?.asRequestBody(config.mediaType)

        return okHttpClient.newCall(
            Request.Builder()
                .headers(config.headers.toHeaders())
                .method(config.method, requestBody)
                .url(config.endpoint)
                .build(),
        )
    }

    private fun executeScriptIfExisting(dbConfiguration: SquitDatabaseConfiguration, path: Path): Boolean {
        if (!Files.exists(path)) return true

        return try {
            dbConnections
                .createOrGet(dbConfiguration.jdbcAddress, dbConfiguration.username, dbConfiguration.password)
                .executeScript(path)

            true
        } catch (error: SQLException) {
            logger.newLineIfNeeded()
            logger.warn(
                "Could not run database script ${path.fileName} for test " +
                    "${path.parent.cut(processedSourcesDir.asPath)} (${error.toString().trim()})",
            )

            false
        }
    }
}
