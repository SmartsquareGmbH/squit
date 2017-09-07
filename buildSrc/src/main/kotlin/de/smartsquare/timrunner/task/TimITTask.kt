package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimITReportContainer
import de.smartsquare.timrunner.entity.TimITReportContainerImpl
import de.smartsquare.timrunner.entity.TimITResult
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.FilesUtils
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

open class TimITTask : DefaultTask(), Reporting<TimITReportContainer> {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputSourceDirectory: Path = Paths.get(project.buildDir.path, "source")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var inputResponseDirectory: Path = Paths.get(project.buildDir.path, "results/processed")

    @get:Internal
    private val internalReports by lazy { TimITReportContainerImpl(this) }

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

        val failedTests = results.filter { it.result.isNotEmpty() }

        if (failedTests.isNotEmpty()) {
            throw GradleException("There ${if (failedTests.size == 1) "was" else "were"} ${failedTests.size} " +
                    "failing ${if (failedTests.size == 1) "test" else "tests"}.")
        }
    }

    private fun runTests(): List<TimITResult> {
        val resultList = arrayListOf<TimITResult>()

        FilesUtils.getLeafDirectories(inputResponseDirectory).forEach { responseDirectory ->
            val actualResponseFile = FilesUtils.validateExistence(responseDirectory.resolve(RESPONSE))
            val expectedResponseFile = FilesUtils.validateExistence(inputSourceDirectory
                    .resolve(responseDirectory.cut(inputResponseDirectory))
                    .resolve(RESPONSE))

            val diffBuilder = DiffBuilder.compare(Input.fromStream(Files.newInputStream(actualResponseFile)))
                    .withTest(Input.fromStream(Files.newInputStream(expectedResponseFile)))
                    .checkForSimilar()
                    .build()

            resultList += constructResult(diffBuilder.differences.joinToString(separator = "\n"), responseDirectory)
        }

        return resultList
    }

    private fun writeXmlReport(result: List<TimITResult>, report: Report) {
        val destinationDirectory = Files.createDirectories(report.destination.toPath())
        val reportFile = FilesUtils.createFileIfNotExists(destinationDirectory.resolve("main.xml"))

        val document = DocumentHelper.createDocument()
        val root = document.addElement("results")

        result.sortedBy { it.path }.groupBy { it.path }.forEach { suite, tests ->
            val suiteElement = root.addElement("suite").apply { addAttribute("name", suite) }

            tests.sortedBy { it.test }.forEach {
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

    private fun constructResult(differences: String, responseDirectory: Path) = if (differences.isNotBlank()) {
        TimITResult(responseDirectory.parent.parent.cut(inputResponseDirectory).toString(),
                responseDirectory.parent.fileName.toString(), responseDirectory.fileName.toString(), differences)
    } else {
        TimITResult(responseDirectory.parent.parent.cut(inputResponseDirectory).toString(),
                responseDirectory.parent.fileName.toString(), responseDirectory.fileName.toString())
    }

    @Internal
    override fun getReports() = internalReports

    override fun reports(closure: Closure<*>) = reports(ClosureBackedAction<TimITReportContainer>(closure))

    override fun reports(configureAction: Action<in TimITReportContainer>) = internalReports.apply {
        configureAction.execute(this)
    }
}
