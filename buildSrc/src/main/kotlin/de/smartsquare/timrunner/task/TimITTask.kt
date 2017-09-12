package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimITReportContainer
import de.smartsquare.timrunner.entity.TimITReportContainerImpl
import de.smartsquare.timrunner.entity.TimITResult
import de.smartsquare.timrunner.io.FilesUtils
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.write
import groovy.lang.Closure
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task for comparing the actual responses to the expected responses and generating a report.
 */
open class TimITTask : DefaultTask(), Reporting<TimITReportContainer> {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var sourcesPath: Path = Paths.get(project.buildDir.path, "source")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var actualResponsesPath: Path = Paths.get(project.buildDir.path, "results/processed")

    @get:Internal
    private val internalReports by lazy { TimITReportContainerImpl(this) }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        val results = runTests()

        internalReports.forEach {
            if (it.isEnabled) {
                when (it.name) {
                    "xml" -> writeXmlReport(results, it)
                    "html" -> writeHtmlReport(results, it)
                    else -> throw GradleException("Unknown report type: ${it.name}")
                }
            }
        }

        val successfulTests = results.count { it.result.isEmpty() }
        val failedTests = results.count { it.result.isNotEmpty() }

        println("${results.size} tests ran.\n$successfulTests successful and $failedTests failed.")

        if (failedTests > 0) throw GradleException("Failing tests.")
    }

    private fun runTests(): List<TimITResult> {
        val resultList = arrayListOf<TimITResult>()

        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { actualResponsePath ->
            val actualResponseFilePath = FilesUtils.validateExistence(actualResponsePath.resolve(RESPONSE))
            val expectedResponseFilePath = FilesUtils.validateExistence(sourcesPath
                    .resolve(actualResponsePath.cut(actualResponsesPath))
                    .resolve(RESPONSE))

            val diffBuilder = DiffBuilder.compare(Input.fromStream(Files.newInputStream(actualResponseFilePath)))
                    .withTest(Input.fromStream(Files.newInputStream(expectedResponseFilePath)))
                    .checkForSimilar()
                    .build()

            resultList += constructResult(diffBuilder.differences.joinToString("\n"), actualResponsePath)
        }

        return resultList
    }

    private fun writeXmlReport(result: List<TimITResult>, report: Report) {
        val destinationDirectory = Files.createDirectories(report.destination.toPath())
        val reportFile = FilesUtils.createFileIfNotExists(destinationDirectory.resolve("main.xml"))

        val document = DocumentHelper.createDocument()
        val root = document.addElement("results")

        result.groupBy { it.path }.forEach { suite, tests ->
            val suiteElement = root.addElement("suite").apply { addAttribute("name", suite) }

            tests.forEach {
                val testElement = suiteElement.addElement("test").apply { addAttribute("name", it.test) }

                when {
                    it.result.isEmpty() -> testElement.addElement("success")
                    else -> testElement.addElement("failure").addText(it.result)
                }
            }
        }

        document.write(reportFile, OutputFormat.createPrettyPrint())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun writeHtmlReport(result: List<TimITResult>, report: Report) {
        TODO()
    }

    private fun constructResult(differences: String, expectedResponsePath: Path) = when (differences.isNotBlank()) {
        true -> TimITResult(expectedResponsePath.parent.parent.cut(actualResponsesPath).toString(),
                expectedResponsePath.parent.fileName.toString(), expectedResponsePath.fileName.toString(), differences)

        false -> TimITResult(expectedResponsePath.parent.parent.cut(actualResponsesPath).toString(),
                expectedResponsePath.parent.fileName.toString(), expectedResponsePath.fileName.toString())
    }

    @Internal
    override fun getReports() = internalReports

    override fun reports(closure: Closure<*>) = reports(ClosureBackedAction<TimITReportContainer>(closure))

    override fun reports(configureAction: Action<in TimITReportContainer>) = internalReports.apply {
        configureAction.execute(this)
    }
}
