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

open class TimTransformerTask : DefaultTask() {

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var inputDirectory = File("${project.buildDir}/results/raw")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory = File("${project.buildDir}/results/processed")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        inputDirectory.listFiles(DirectoryFilter()).forEach { suiteDir ->
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

                val document = SAXReader().read(responseFile).let {
                    transform(it)
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
    private fun transform(document: Document): Document {
        return document
    }
}
