package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.DirectoryFilter
import de.smartsquare.timrunner.util.use
import groovy.lang.Closure
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.io.File

open class TimITTask : DefaultTask(), Reporting<TimITReportContainer> {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputSourceDirectory = File(project.projectDir, "src/main/test")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var inputResponseDirectory = File(project.buildDir, "results/processed")

    @Suppress("LeakingThis")
    @Internal
    private val reports = TimITReportContainerImpl(this)

    @TaskAction
    fun run() {
        val results = runTests()

        reports.forEach {
            if (it.isEnabled) {
                when (it.name) {
                    "xml" -> writeXmlReport(results, it)
                    "html" -> writeHtmlReport(results, it)
                    else -> throw GradleException("Unknown report type: ${it.name}")
                }
            }
        }

        val failedTests = results.sortedBy { it.suite }.filter { it.result.isNotEmpty() }

        if (failedTests.isNotEmpty()) {
            throw GradleException("There ${if (failedTests.size == 1) "was" else "were"} ${failedTests.size} " +
                    "failing ${if (failedTests.size == 1) "test" else "tests"}.")
        }
    }

    private fun runTests(): List<TimITResult> {
        val resultList = arrayListOf<TimITResult>()

        inputResponseDirectory.listFiles(DirectoryFilter()).forEach { suiteDir ->
            suiteDir.listFiles(DirectoryFilter()).forEach { testDir ->
                val actualResponseFile = File(testDir, "response.xml").also {
                    if (!it.exists()) throw GradleException("Inconsistency detected for test: ${testDir.name}")
                }

                val expectedResponseFile = File(inputSourceDirectory, "${suiteDir.name}/${testDir.name}/response.xml")
                        .also {
                            if (!it.exists()) throw GradleException("Inconsistency detected for test: ${testDir.name}")
                        }

                val diffBuilder = DiffBuilder.compare(Input.fromFile(expectedResponseFile))
                        .withTest(Input.fromFile(actualResponseFile))
                        .build()

                resultList += if (diffBuilder.hasDifferences()) {
                    TimITResult(suiteDir.name, testDir.name, diffBuilder.differences.joinToString(separator = "\n\n"))
                } else {
                    TimITResult(suiteDir.name, testDir.name)
                }
            }
        }

        return resultList
    }

    private fun writeXmlReport(result: List<TimITResult>, report: Report) {
        val destinationDirectory = report.destination.also {
            if (!it.exists() && !it.mkdirs()) throw GradleException("Could not create report directory.")
        }

        val reportFile = File(destinationDirectory, "main.xml").also {
            if (!it.exists() && !it.createNewFile()) throw GradleException("Could not create report file.")
        }

        val document = DocumentHelper.createDocument()
        val root = document.addElement("results")

        result.sortedBy { it.suite }.forEach {
            val suiteElement = root.addElement("suite").apply {
                addAttribute("name", it.suite)
            }

            val testElement = suiteElement.addElement("test").apply {
                addAttribute("name", it.test)
            }

            if (it.result.isEmpty()) {
                testElement.addElement("failure").addText(it.result)
            } else {
                testElement.addElement("success")
            }
        }

        XMLWriter(reportFile.bufferedWriter(), OutputFormat.createPrettyPrint()).use {
            it.write(document)
        }
    }

    private fun writeHtmlReport(result: List<TimITResult>, report: Report) {
        throw NotImplementedError()
    }

    override fun getReports() = reports

    override fun reports(closure: Closure<*>) = reports(ClosureBackedAction<TimITReportContainer>(closure))

    override fun reports(configureAction: Action<in TimITReportContainer>) = reports.apply {
        configureAction.execute(this)
    }
}
