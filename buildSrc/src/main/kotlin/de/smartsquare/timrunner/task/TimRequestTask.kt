package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.DirectoryFilter
import de.smartsquare.timrunner.util.TimApi
import okhttp3.HttpUrl
import oracle.jdbc.driver.OracleDriver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.sql.DriverManager
import java.util.*

/**
 * Task for running requests against the tim API.
 *
 * @author Ruben Gees
 */
open class TimRequestTask : DefaultTask() {

    /**
     * The scheme of the API.
     */
    @Input
    var scheme = "http"

    /**
     * The host of the API.
     */
    @Input
    var host = "localhost"

    /**
     * The port of the API.
     */
    @Input
    var port = 7001

    /**
     * Additional path segments of the API. *Must* end with a backslash.
     */
    @Input
    var pathSegments = "tim/"

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputDirectory = File("${project.projectDir}/src/main/test")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory = File("${project.buildDir}/results/raw")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        DriverManager.registerDriver(OracleDriver())
        DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "timdb", "timdb").use { timdbConnection ->
            DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "timstat", "timstat").use { timstatConnection ->

                inputDirectory.listFiles(DirectoryFilter()).forEach { suiteDir ->
                    val propertiesFile = File(suiteDir, "config.properties").also {
                        if (!it.exists()) throw GradleException("No config.properties file for suite: ${suiteDir.name}")
                    }

                    val endpoint = Properties().apply { load(propertiesFile.inputStream()) }.getProperty("endpoint")

                    suiteDir.listFiles(DirectoryFilter()).forEach { testDir ->
                        val preSqlFile = File(testDir, "pre.sql")
                        val postSqlFile = File(testDir, "post.sql")
                        val requestFile = File(testDir, "request.xml").also {
                            if (!it.exists()) throw GradleException("No request.xml file for test: ${testDir.name}")
                        }

                        if (preSqlFile.exists()) {
                            preSqlFile.readText().split(";").filter { it.isNotBlank() }.forEach { query ->
                                timdbConnection.createStatement().use { statement ->
                                    statement.execute(query)
                                }

                                timdbConnection.commit()
                            }
                        }

                        val soapResponse = constructApi().request(endpoint, requestFile.readText())
                                .execute()
                                .let { response ->
                                    if (!response.isSuccessful) {
                                        throw GradleException("Could not request tim for test: ${testDir.name}")
                                    }

                                    response.body()?.string() ?: throw GradleException("Empty response for test: ${testDir.name}")
                                }

                        if (postSqlFile.exists()) {
                            postSqlFile.readText().split(";").filter { it.isNotBlank() }.forEach { query ->
                                timdbConnection.createStatement().use { statement ->
                                    statement.execute(query)
                                }

                                timdbConnection.commit()
                            }
                        }

                        val resultDir = File(outputDirectory, "${suiteDir.name}/${testDir.name}").also {
                            if (!it.exists() && !it.mkdirs()) throw GradleException("Couldn't create result directory")
                        }

                        val resultFile = File(resultDir, "response.xml").also {
                            if (!it.exists() && !it.createNewFile()) throw GradleException("Couldn't create result file")
                        }

                        resultFile.writeText(soapResponse)
                    }
                }
            }
        }
    }

    private fun constructApi() = Retrofit.Builder()
            .baseUrl(HttpUrl.Builder()
                    .scheme(scheme)
                    .host(host)
                    .port(port)
                    .addPathSegments(pathSegments)
                    .build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(TimApi::class.java)
}
