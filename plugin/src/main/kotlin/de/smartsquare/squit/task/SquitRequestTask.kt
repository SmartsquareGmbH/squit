package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.db.ConnectionCollection
import de.smartsquare.squit.db.executeScript
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.REQUEST
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.printAndFlush
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException
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
    val jdbcDriverClassName by lazy {
        extension.jdbcDriver?.let {
            if (it.isNotBlank()) {
                logger.info("Using $it for jdbc connections.")

                it
            } else {
                null
            }
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

    @get:Internal
    private val okHttpClient = OkHttpClient.Builder().build()

    @get:Internal
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
        jdbcDriverClassName?.let {
            DriverManager.registerDriver(Class.forName(it).newInstance() as Driver)
        }

        FilesUtils.deleteRecursivelyIfExisting(actualResponsesPath)
        Files.createDirectories(actualResponsesPath)

        dbConnections.use {
            FilesUtils.getSortedLeafDirectories(processedSourcesPath).forEachIndexed { index, testDirectoryPath ->
                if (logger.isLifecycleEnabled) {
                    printAndFlush("\rRunning test $index")
                }

                val propertiesPath = FilesUtils.validateExistence(testDirectoryPath.resolve(CONFIG))
                val properties = SquitProperties().fillFromSingleProperties(propertiesPath)

                val requestPath = FilesUtils.validateExistence(testDirectoryPath.resolve(REQUEST))

                doRequestAndScriptExecutions(testDirectoryPath, requestPath, properties)
            }
        }

        println()
    }

    private fun doRequestAndScriptExecutions(testDirectoryPath: Path, requestPath: Path, properties: SquitProperties) {
        properties.databaseConfigurations.forEach {
            executeScriptIfExisting(testDirectoryPath.resolve("${it.name}_pre.sql"), it.jdbcAddress,
                    it.username, it.password)
        }

        val soapResponse: String = constructApiCall(properties.endpoint, requestPath, properties.mediaType)
                .execute()
                .let { response ->
                    if (!response.isSuccessful) {
                        response.message()
                    }

                    response.body()?.string() ?: ""
                }

        val resultResponsePath = Files.createDirectories(actualResponsesPath
                .resolve(testDirectoryPath.cut(processedSourcesPath)))

        val resultResponseFilePath = FilesUtils.createFileIfNotExists(resultResponsePath.resolve(ACTUAL_RESPONSE))

        Files.write(resultResponseFilePath, soapResponse.toByteArray(Charsets.UTF_8))

        properties.databaseConfigurations.forEach {
            executeScriptIfExisting(testDirectoryPath.resolve("${it.name}_post.sql"), it.jdbcAddress,
                    it.username, it.password)
        }
    }

    private fun constructApiCall(url: HttpUrl, requestPath: Path, mediaType: MediaType) = okHttpClient
            .newCall(Request.Builder()
                    .post(RequestBody.create(mediaType, Files.readAllBytes(requestPath)))
                    .url(url)
                    .build()
            )

    private fun executeScriptIfExisting(path: Path, jdbc: String, username: String, password: String): Boolean {
        return if (Files.exists(path)) {
            try {
                dbConnections.createOrGet(jdbc, username, password).executeScript(path)

                true
            } catch (error: SQLException) {
                if (logger.isLifecycleEnabled) {
                    // Switch to next line from progress.
                    println()
                }

                logger.warn("Could not run database script ${path.fileName} for test " +
                        "${path.parent.cut(processedSourcesPath)} (${error.toString().trim()})")

                false
            }
        } else {
            true
        }
    }
}
