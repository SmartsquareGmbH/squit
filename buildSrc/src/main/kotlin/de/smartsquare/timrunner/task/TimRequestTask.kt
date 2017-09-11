package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.util.*
import de.smartsquare.timrunner.util.Constants.CONFIG
import de.smartsquare.timrunner.util.Constants.REQUEST
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_POST
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_PRE
import de.smartsquare.timrunner.util.Constants.TIM_DB_POST
import de.smartsquare.timrunner.util.Constants.TIM_DB_PRE
import okhttp3.*
import oracle.jdbc.driver.OracleDriver
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager

/**
 * Task for running requests against the tim API.
 *
 * @author Ruben Gees
 */
open class TimRequestTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputPath: Path = Paths.get(project.buildDir.path, "source")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputPath: Path = Paths.get(project.buildDir.path, "results/raw")

    @Internal
    private val okHttpClient = OkHttpClient.Builder().build()

    @Internal
    private val dbConnections = ConnectionCollection()

    init {
        DriverManager.registerDriver(OracleDriver())
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        outputPath.toFile().deleteRecursively()

        Files.createDirectories(outputPath)

        dbConnections.use {
            FilesUtils.getLeafDirectories(inputPath)
                    .sortedWith(Comparator { first, second ->
                        Utils.getTestIndex(first).compareTo(Utils.getTestIndex(second))
                    })
                    .forEachIndexed { index, testDirectoryPath ->
                        logger.quiet("Running test ${index + 1}")

                        val propertiesPath = FilesUtils.validateExistence(testDirectoryPath.resolve(CONFIG))
                        val properties = TimProperties().fillFromSingleProperties(propertiesPath)

                        val requestPath = FilesUtils.validateExistence(testDirectoryPath.resolve(REQUEST))

                        doRequestAndScriptExecutions(testDirectoryPath, requestPath, properties)
                    }
        }
    }

    private fun doRequestAndScriptExecutions(testDirectoryPath: Path, requestPath: Path, properties: TimProperties) {
        executeScriptIfExisting(testDirectoryPath.resolve(TIM_DB_PRE), properties.timdbJdbc,
                properties.timdbUser, properties.timdbPassword)
        executeScriptIfExisting(testDirectoryPath.resolve(TAXBASE_DB_PRE), properties.taxbasedbJdbc,
                properties.taxbasedbUser, properties.taxbasedbPassword)

        val soapResponse = constructApiCall(properties.endpoint, requestPath)
                .execute()
                .let { response ->
                    if (!response.isSuccessful) {
                        response.message()
                    }

                    response.body()?.string() ?: ""
                }

        val resultDirectoryPath = Files.createDirectories(outputPath.resolve(testDirectoryPath.cut(inputPath)))
        val resultFilePath = FilesUtils.createFileIfNotExists(resultDirectoryPath.resolve(RESPONSE))

        Files.write(resultFilePath, soapResponse.toByteArray(Charsets.UTF_8))

        executeScriptIfExisting(testDirectoryPath.resolve(TIM_DB_POST), properties.timdbJdbc,
                properties.timdbUser, properties.timdbPassword)
        executeScriptIfExisting(testDirectoryPath.resolve(TAXBASE_DB_POST), properties.taxbasedbJdbc,
                properties.taxbasedbUser, properties.taxbasedbPassword)
    }

    private fun constructApiCall(url: HttpUrl, requestPath: Path) = okHttpClient.newCall(Request.Builder()
            .post(RequestBody.create(MediaType.parse("application/soap+xml; utf-8"), requestPath.toFile()))
            .url(url)
            .build()
    )

    private fun executeScriptIfExisting(path: Path, jdbc: String, username: String, password: String): Boolean {
        return if (Files.exists(path)) {
            try {
                dbConnections.createOrGet(jdbc, username, password).executeScript(path)

                true
            } catch (error: Throwable) {
                logger.warn("Could not run database script ${path.fileName} for test " +
                        "${path.parent.cut(inputPath)} (${error.toString().trim()})")

                false
            }
        } else {
            true
        }
    }
}
