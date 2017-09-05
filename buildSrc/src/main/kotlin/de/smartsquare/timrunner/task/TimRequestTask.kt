package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.util.ConnectionCollection
import de.smartsquare.timrunner.util.FilesUtils
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.executeScript
import okhttp3.*
import oracle.jdbc.driver.OracleDriver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
        dbConnections.use {
            FilesUtils.getLeafDirectories(inputPath).forEach { testDir ->
                val propertiesPath = FilesUtils.createFileIfNotExists(testDir.resolve("config.properties"))
                val properties = TimProperties().fillFromProperties(propertiesPath)

                val requestPath = FilesUtils.validateExistence(testDir.resolve("request.xml"))

                executeScriptIfExisting(testDir.resolve("timdb_pre.sql"), properties.timdbJdbc,
                        properties.timdbUser, properties.timdbPassword)
                executeScriptIfExisting(testDir.resolve("timstat_pre.sql"), properties.timstatJdbc,
                        properties.timstatUser, properties.timstatPassword)

                val soapResponse = constructApiCall(properties.endpoint, requestPath)
                        .execute()
                        .let { response ->
                            if (!response.isSuccessful) {
                                throw GradleException("Could not request tim for test: ${testDir.fileName} " +
                                        "(${response.message()})")
                            }

                            response.body()?.string() ?: throw GradleException("Empty response for test: " +
                                    "${testDir.fileName}")
                        }

                val resultDirectoryPath = Files.createDirectories(outputPath.resolve(testDir.cut(inputPath)))
                val resultFilePath = FilesUtils.createFileIfNotExists(resultDirectoryPath.resolve("response.xml"))

                Files.write(resultFilePath, soapResponse.lines())

                executeScriptIfExisting(testDir.resolve("timdb_post.sql"), properties.timdbJdbc,
                        properties.timdbUser, properties.timdbPassword)
                executeScriptIfExisting(testDir.resolve("timstat_post.sql"), properties.timstatJdbc,
                        properties.timstatUser, properties.timstatPassword)
            }
        }
    }

    private fun constructApiCall(url: HttpUrl, requestPath: Path) = okHttpClient.newCall(Request.Builder()
            .post(RequestBody.create(MediaType.parse("application/soap+xml"), requestPath.toFile()))
            .url(url)
            .build()
    )

    private fun executeScriptIfExisting(path: Path, jdbc: String, username: String, password: String) {
        if (Files.exists(path)) {
            dbConnections.createOrGet(jdbc, username, password).executeScript(path)
        }
    }
}
