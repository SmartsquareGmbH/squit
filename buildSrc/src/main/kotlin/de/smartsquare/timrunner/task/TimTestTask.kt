package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimITResult
import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.io.FilesUtils
import de.smartsquare.timrunner.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.timrunner.util.Constants.CONFIG
import de.smartsquare.timrunner.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.write
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task for comparing the actual responses to the expected responses and generating a report.
 */
open class TimTestTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var processedSourcesPath: Path = Paths.get(project.buildDir.path, "sources")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var actualResponsesPath: Path = Paths.get(project.buildDir.path, "responses", "processed")

    /**
     * The directory to generate the xml report file into.
     */
    @OutputFile
    var xmlReportFilePath: Path = Paths.get(project.buildDir.path, "reports", "main.xml")

    /**
     * The directory to copy failed tests into.
     */
    @OutputDirectory
    var failureResultDirectory: Path = Paths.get(project.buildDir.path, "reports", "failures")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        val results = runTests()

        writeXmlReport(results)
        copyFailures(results)

        val successfulTests = results.count { it.isSuccess }
        val failedTests = results.count { !it.isSuccess }

        println("${results.size} tests ran.\n$successfulTests successful and $failedTests failed.")

        if (failedTests > 0) throw GradleException("Failing tests.")
    }

    private fun runTests(): List<TimITResult> {
        val resultList = arrayListOf<TimITResult>()

        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { actualResponsePath ->
            val propertiesPath = FilesUtils.validateExistence(processedSourcesPath
                    .resolve(actualResponsePath.cut(actualResponsesPath)).resolve(CONFIG))

            val properties = TimProperties().fillFromSingleProperties(propertiesPath)

            if (!properties.ignoreForReport) {
                val actualResponseFilePath = FilesUtils.validateExistence(actualResponsePath.resolve(ACTUAL_RESPONSE))
                val expectedResponseFilePath = FilesUtils.validateExistence(processedSourcesPath
                        .resolve(actualResponsePath.cut(actualResponsesPath))
                        .resolve(EXPECTED_RESPONSE))

                val diffBuilder = DiffBuilder.compare(Input.fromStream(Files.newInputStream(actualResponseFilePath)))
                        .withTest(Input.fromStream(Files.newInputStream(expectedResponseFilePath)))
                        .ignoreWhitespace()
                        .checkForSimilar()
                        .build()

                resultList += constructResult(diffBuilder.differences.joinToString("\n"), actualResponsePath)
            } else {
                logger.warn("Ignoring test for report ${actualResponsePath.cut(actualResponsesPath)}")
            }
        }

        return resultList
    }

    private fun writeXmlReport(result: List<TimITResult>) {
        Files.createDirectories(xmlReportFilePath.parent)

        val reportFilePath = FilesUtils.createFileIfNotExists(xmlReportFilePath)

        val document = DocumentHelper.createDocument()
        val root = document.addElement("results")

        result.groupBy { it.path }.forEach { suite, tests ->
            val suiteElement = root.addElement("suite").apply {
                addAttribute("name", suite.toString())
            }

            tests.forEach {
                val testElement = suiteElement.addElement("test").apply {
                    addAttribute("name", it.testDirectoryPath.toString())
                }

                when (it.isSuccess) {
                    true -> testElement.addElement("success")
                    false -> testElement.addElement("failure").addText(it.result)
                }
            }
        }

        document.write(reportFilePath, OutputFormat.createPrettyPrint())
    }

    private fun copyFailures(result: List<TimITResult>) {
        FilesUtils.deleteRecursivelyIfExisting(failureResultDirectory)
        Files.createDirectories(failureResultDirectory)

        result.filterNot { it.isSuccess }.forEach {
            val resultDirectoryPath = Files.createDirectories(failureResultDirectory.resolve(it.fullPath))

            val testProcessedSourcesPath = FilesUtils.validateExistence(processedSourcesPath.resolve(it.fullPath))
            val testActualResponsesPath = FilesUtils.validateExistence(actualResponsesPath.resolve(it.fullPath))

            FilesUtils.copyFilesFromDirectory(testProcessedSourcesPath, resultDirectoryPath)
            FilesUtils.copyFilesFromDirectory(testActualResponsesPath, resultDirectoryPath)
        }
    }

    private fun constructResult(differences: String, expectedResponsePath: Path) = when (differences.isNotBlank()) {
        true -> TimITResult(expectedResponsePath.parent.parent.cut(actualResponsesPath),
                expectedResponsePath.parent.fileName, expectedResponsePath.fileName, differences)

        false -> TimITResult(expectedResponsePath.parent.parent.cut(actualResponsesPath),
                expectedResponsePath.parent.fileName, expectedResponsePath.fileName)
    }
}
