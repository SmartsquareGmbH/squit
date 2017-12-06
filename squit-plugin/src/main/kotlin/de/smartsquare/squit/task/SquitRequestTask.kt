package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.db.ConnectionCollection
import de.smartsquare.squit.db.executeScript
import de.smartsquare.squit.entity.SquitMetaInfo
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.REQUEST
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.databaseConfigurations
import de.smartsquare.squit.util.endpoint
import de.smartsquare.squit.util.lifecycleOnSameLine
import de.smartsquare.squit.util.mediaType
import de.smartsquare.squit.util.newLineIfNeeded
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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
    @Suppress("MemberVisibilityCanPrivate")
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
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val actualResponsesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)

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
            DriverManager.registerDriver(Class.forName(it).newInstance() as Driver)
        }

        FilesUtils.deleteRecursivelyIfExisting(actualResponsesPath)
        Files.createDirectories(actualResponsesPath)

        dbConnections.use {
            FilesUtils.getSortedLeafDirectories(processedSourcesPath).forEachIndexed { index, testDirectoryPath ->
                logger.lifecycleOnSameLine("Running test $index", project.gradle.startParameter.consoleOutput)

                val configPath = FilesUtils.validateExistence(testDirectoryPath.resolve(CONFIG))
                val config = ConfigFactory.parseFile(configPath.toFile())

                val requestPath = FilesUtils.validateExistence(testDirectoryPath.resolve(REQUEST))

                doRequestAndScriptExecutions(testDirectoryPath, requestPath, config)
            }
        }

        logger.newLineIfNeeded()
    }

    private fun doRequestAndScriptExecutions(testDirectoryPath: Path, requestPath: Path, config: Config) {
        val resultResponsePath = Files.createDirectories(actualResponsesPath
                .resolve(testDirectoryPath.cut(processedSourcesPath)))

        val resultResponseFilePath = FilesUtils.createFileIfNotExists(resultResponsePath.resolve(ACTUAL_RESPONSE))
        val metaFilePath = FilesUtils.createFileIfNotExists(resultResponsePath.resolve(META))

        val startTime = System.currentTimeMillis()

        config.databaseConfigurations.forEach { (name, jdbcAddress, username, password) ->
            executeScriptIfExisting(testDirectoryPath.resolve("${name}_pre.sql"), jdbcAddress, username, password)
        }

        try {
            val soapResponse = constructApiCall(config.endpoint, requestPath, config.mediaType)
                    .execute()
                    .body()
                    ?.string() ?: ""

            Files.write(resultResponseFilePath, soapResponse.toByteArray(Charsets.UTF_8))
        } catch (error: IOException) {
            logger.newLineIfNeeded()
            logger.warn("Request failed for test ${requestPath.parent.cut(processedSourcesPath)} " +
                    "(${error.toString().trim()})")
        }

        config.databaseConfigurations.forEach { (name, jdbcAddress, username, password) ->
            executeScriptIfExisting(testDirectoryPath.resolve("${name}_post.sql"), jdbcAddress, username, password)
        }

        val endTime = System.currentTimeMillis()
        val metaInfo = SquitMetaInfo(LocalDateTime.now(), endTime - startTime)

        Files.write(metaFilePath, metaInfo.toJson().toByteArray())
    }

    private fun constructApiCall(url: HttpUrl, requestPath: Path, mediaType: MediaType) = okHttpClient
            .newCall(Request.Builder()
                    .post(RequestBody.create(mediaType, Files.readAllBytes(requestPath)))
                    .url(url)
                    .build()
            )

    @Suppress("ExpressionBodySyntax")
    private fun executeScriptIfExisting(path: Path, jdbc: String, username: String, password: String): Boolean {
        return if (Files.exists(path)) {
            try {
                dbConnections.createOrGet(jdbc, username, password).executeScript(path)

                true
            } catch (error: SQLException) {
                logger.newLineIfNeeded()
                logger.warn("Could not run database script ${path.fileName} for test " +
                        "${path.parent.cut(processedSourcesPath)} (${error.toString().trim()})")

                false
            }
        } else {
            true
        }
    }
}
