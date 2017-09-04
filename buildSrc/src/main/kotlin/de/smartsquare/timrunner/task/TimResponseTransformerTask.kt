package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.DirectoryFilter
import de.smartsquare.timrunner.util.TimOutputFormat
import de.smartsquare.timrunner.util.use
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TimResponseTransformerTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputSourceDirectory = File(project.projectDir, "src/main/test")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var inputResponseDirectory = File(project.buildDir, "results/raw")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory = File(project.buildDir, "results/processed")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        inputResponseDirectory.listFiles(DirectoryFilter()).forEach { suiteDir ->
            suiteDir.listFiles(DirectoryFilter()).forEach { testDir ->
                val responseFile = File(testDir, "response.xml").also {
                    if (!it.exists()) throw GradleException("Inconsistency detected for test: ${testDir.name}")
                }

                val resultDir = File(outputDirectory, "${suiteDir.name}/${testDir.name}").also {
                    if (!it.exists() && !it.mkdirs()) throw GradleException("Couldn't create result directory")
                }

                val resultFile = File(resultDir, "response.xml").also {
                    if (!it.exists() && !it.createNewFile()) throw GradleException("Couldn't create result file")
                }

                val expectedResponseFile = File(inputSourceDirectory, "${suiteDir.name}/${testDir.name}/response.xml")
                        .also {
                            if (!it.exists()) throw GradleException("Inconsistency detected for test: ${testDir.name}")
                        }

                val document = SAXReader().read(responseFile).let { responseDocument ->
                    SAXReader().read(expectedResponseFile).let { expectedResponseDocument ->
                        transform(responseDocument, expectedResponseDocument)
                    }
                }

                XMLWriter(resultFile.bufferedWriter(), TimOutputFormat()).use {
                    it.write(document)
                }
            }
        }
    }

    /**
     * TODO
     */
    private fun transform(response: Document, expectedResponse: Document): Document {
        if (response.selectNodes("Fault").isNotEmpty()) return response

        expectedResponse.selectSingleNode("//TransactionId").let {
            response.selectSingleNode("//TransactionId").text = it.text
        }

        return response
    }
}
