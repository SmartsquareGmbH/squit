package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.write
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * Task for comparing the actual responses to the expected responses and generating a report.
 *
 * @author Ruben Gees
 */
open class SquitTestTask : DefaultTask() {

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path, "squit", "sources")

    /**
     * The directory of the previously requested responses.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val actualResponsesPath: Path = Paths.get(project.buildDir.path, "squit", "responses", "processed")

    /**
     * The directory to generate the xml report file into.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputFile
    val xmlReportFilePath by lazy {
        extension.reportsPath?.resolve("main.xml")
                ?: throw IllegalArgumentException("reportPath cannot be null")
    }

    /**
     * The directory to copy failed tests into.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val failureResultDirectory by lazy {
        extension.reportsPath?.resolve("failures")
                ?: throw IllegalArgumentException("reportPath cannot be null")
    }

    init {
        group = "Build"
        description = "Compares the actual responses to the expected responses and generates a report."
    }

    /**
     * Runs the task.
     */
    @Suppress("unused")
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

    private fun runTests(): List<SquitResult> {
        val resultList = arrayListOf<SquitResult>()

        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { actualResponsePath ->
            val propertiesPath = FilesUtils.validateExistence(processedSourcesPath
                    .resolve(actualResponsePath.cut(actualResponsesPath)).resolve(CONFIG))

            val properties = SquitProperties().fillFromSingleProperties(propertiesPath)

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

    private fun writeXmlReport(result: List<SquitResult>) {
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

    private fun copyFailures(result: List<SquitResult>) {
        FilesUtils.deleteRecursivelyIfExisting(failureResultDirectory)
        Files.createDirectories(failureResultDirectory)

        result.filterNot { it.isSuccess }.forEach {
            val resultDirectoryPath = Files.createDirectories(failureResultDirectory.resolve(it.fullPath))

            val testProcessedSourcesPath = FilesUtils.validateExistence(processedSourcesPath.resolve(it.fullPath))
            val testActualResponsesPath = FilesUtils.validateExistence(actualResponsesPath.resolve(it.fullPath))
            val testDifferenceFile = Files.createFile(resultDirectoryPath.resolve("diff.txt"))

            FilesUtils.copyFilesFromDirectory(testProcessedSourcesPath, resultDirectoryPath)
            FilesUtils.copyFilesFromDirectory(testActualResponsesPath, resultDirectoryPath)
            Files.write(testDifferenceFile, it.result.toByteArray())
        }
    }

    private fun constructResult(differences: String, expectedResponsePath: Path) = when (differences.isNotBlank()) {
        true -> SquitResult(expectedResponsePath.parent.parent.cut(actualResponsesPath),
                expectedResponsePath.parent.fileName, expectedResponsePath.fileName, differences)

        false -> SquitResult(expectedResponsePath.parent.parent.cut(actualResponsesPath),
                expectedResponsePath.parent.fileName, expectedResponsePath.fileName)
    }
}
