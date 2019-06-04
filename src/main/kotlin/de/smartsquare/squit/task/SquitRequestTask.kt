package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.db.ConnectionCollection
import de.smartsquare.squit.db.executeScript
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
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.databaseConfigurations
import de.smartsquare.squit.util.endpoint
import de.smartsquare.squit.util.headers
import de.smartsquare.squit.util.lifecycleOnSameLine
import de.smartsquare.squit.util.mediaType
import de.smartsquare.squit.util.method
import de.smartsquare.squit.util.newLineIfNeeded
import de.smartsquare.squit.util.postRunnerScripts
import de.smartsquare.squit.util.postRunners
import de.smartsquare.squit.util.preRunnerScripts
import de.smartsquare.squit.util.preRunners
import groovy.lang.Binding
import groovy.lang.GroovyShell
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.internal.http.HttpMethod
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Task for running requests against the given api. Also capable of running existing sql scripts before and after the
 * request.
 *
 * @author Ruben Gees
 */
open class SquitRequestTask : DefaultTask() {

    /**
     * The class name of the jdbc [Driver] to use.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Input
    val jdbcDriverClassNames by lazy {
        extension.jdbcDrivers
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .let {
                logger.info("Using $it for jdbc connections.")

                it
            }
    }

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:InputDirectory
    val processedSourcesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY,
        SOURCES_DIRECTORY
    )

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputDirectory
    val actualResponsesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY,
        RESPONSES_DIRECTORY,
        RAW_DIRECTORY
    )

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(extension.timeout, TimeUnit.SECONDS)
            .writeTimeout(extension.timeout, TimeUnit.SECONDS)
            .readTimeout(extension.timeout, TimeUnit.SECONDS)
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
    @Suppress("unused")
    @TaskAction
    fun run() {
        jdbcDriverClassNames.forEach {
            DriverManager.registerDriver(Class.forName(it).getConstructor().newInstance() as Driver)
        }

        FilesUtils.deleteRecursivelyIfExisting(actualResponsesPath)
        Files.createDirectories(actualResponsesPath)

        dbConnections.use {
            FilesUtils.getSortedLeafDirectories(processedSourcesPath).forEachIndexed { index, testDirectoryPath ->
                logger.lifecycleOnSameLine(
                    "Running test ${index + 1}",
                    project.gradle.startParameter.consoleOutput
                )

                val resultResponsePath = Files.createDirectories(
                    actualResponsesPath.resolve(testDirectoryPath.cut(processedSourcesPath))
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
        config: Config
    ) {
        val resultResponseFilePath = resultResponsePath.resolve(MediaTypeFactory.actualResponse(config.mediaType))

        doPreScriptExecutions(config, testDirectoryPath)

        try {
            val apiResponse = constructApiCall(requestPath, config).execute()
            val apiBody = apiResponse.body()?.string() ?: throw IOException("Subject did not answer with a body")
            val mediaType = apiResponse.body()?.contentType()

            Files.write(resultResponseFilePath, apiBody.toByteArray())

            val responseInfo = SquitResponseInfo(apiResponse.code())
            val resultResponseInfoFilePath = resultResponsePath.resolve(ACTUAL_RESPONSE_INFO)
            Files.write(resultResponseInfoFilePath, responseInfo.toJson().toByteArray())

            if (!apiResponse.isSuccessful) {
                logger.newLineIfNeeded()
                logger.info(
                    "Unsuccessful request for test ${testDirectoryPath.cut(processedSourcesPath)} " +
                        "(status code: ${apiResponse.code()})"
                )
            } else if (
                mediaType?.type() != config.mediaType.type() ||
                mediaType?.subtype() != config.mediaType.subtype()
            ) {
                logger.newLineIfNeeded()
                logger.info(
                    "Unexpected Media type $mediaType for test ${testDirectoryPath.cut(processedSourcesPath)}. " +
                        "Expected ${config.mediaType}"
                )
            }
        } catch (error: IOException) {
            Files.write(resultResponsePath.resolve(ERROR), error.toString().toByteArray())
        }

        doPostScriptExecutions(config, testDirectoryPath)
    }

    private fun doPreScriptExecutions(config: Config, testDirectoryPath: Path) {
        config
            .preRunners
            .map {
                preRunnersCache.getOrPut(it) { Class.forName(it).getConstructor().newInstance() as SquitPreRunner }
            }
            .forEach { it.run(config) }

        config.preRunnerScripts.forEach {
            GroovyShell(javaClass.classLoader)
                .parse(Files.newBufferedReader(it))
                .apply { binding = Binding(mapOf("config" to config)) }
                .run()
        }

        config.databaseConfigurations.forEach { (name, jdbcAddress, username, password) ->
            executeScriptIfExisting(testDirectoryPath.resolve("${name}_pre.sql"), jdbcAddress, username, password)
        }
    }

    private fun doPostScriptExecutions(config: Config, testDirectoryPath: Path) {
        config.databaseConfigurations.forEach { (name, jdbcAddress, username, password) ->
            executeScriptIfExisting(testDirectoryPath.resolve("${name}_post.sql"), jdbcAddress, username, password)
        }

        config
            .postRunners
            .map {
                postRunnersCache.getOrPut(it) { Class.forName(it).getConstructor().newInstance() as SquitPostRunner }
            }
            .forEach { it.run(config) }

        config.postRunnerScripts.forEach {
            GroovyShell(javaClass.classLoader)
                .parse(Files.newBufferedReader(it))
                .apply { binding = Binding(mapOf("config" to config)) }
                .run()
        }
    }

    private fun constructApiCall(requestPath: Path?, config: Config): Call {
        val requestBody = requestPath?.let { RequestBody.create(config.mediaType, Files.readAllBytes(requestPath)) }

        return okHttpClient.newCall(
            Request.Builder()
                .headers(Headers.of(config.headers))
                .method(config.method, requestBody)
                .url(config.endpoint)
                .build()
        )
    }

    private fun executeScriptIfExisting(
        path: Path,
        jdbc: String,
        username: String,
        password: String
    ) = if (Files.exists(path)) {
        try {
            dbConnections.createOrGet(jdbc, username, password).executeScript(path)

            true
        } catch (error: SQLException) {
            logger.newLineIfNeeded()
            logger.warn(
                "Could not run database script ${path.fileName} for test " +
                    "${path.parent.cut(processedSourcesPath)} (${error.toString().trim()})"
            )

            false
        }
    } else {
        true
    }
}
