package de.smartsquare.timrunner

import okhttp3.HttpUrl
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
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

    private val api by lazy {
        Retrofit.Builder()
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

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        val testSrcDir = File("${project.projectDir}/src/main/test").also {
            if (!it.exists()) throw GradleException("Test directory not found. The path should be projectDir/src/main/test")
        }

        testSrcDir.listFiles(DirectoryFilter()).forEach { suiteDir ->
            val propertiesFile = File(suiteDir, "config.properties").also {
                if (!it.exists()) throw GradleException("No config.properties file for suite: ${suiteDir.name}")
            }

            val endpoint = Properties().apply { load(propertiesFile.inputStream()) }.getProperty("endpoint")

            suiteDir.listFiles(DirectoryFilter()).forEach { testDir ->
                val requestFile = File(testDir, "request.xml").also {
                    if (!it.exists()) throw GradleException("No request.xml file for test: ${testDir.name}")
                }

                val soapResponse = api.request(endpoint, requestFile.readText())
                        .execute()
                        .let { response ->
                            if (!response.isSuccessful) {
                                throw GradleException("Could not request tim for test: ${testDir.name}")
                            }

                            response.body()?.string() ?: throw GradleException("Empty response for test: ${testDir.name}")
                        }

                val resultDir = File(project.buildDir, "results/${suiteDir.name}/${testDir.name}").also {
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
