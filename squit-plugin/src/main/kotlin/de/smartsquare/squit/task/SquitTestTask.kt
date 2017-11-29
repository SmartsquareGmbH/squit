package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.report.HtmlReportWriter
import de.smartsquare.squit.report.XmlReportWriter
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * Task for comparing the actual responses to the expected responses and generating a report.
 *
 * @author Ruben Gees
 */
@Suppress("StringLiteralDuplication")
open class SquitTestTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The directory of the previously requested responses.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val processedResponsesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, RESPONSES_DIRECTORY, PROCESSED_DIRECTORY)

    /**
     * The directory to generate the xml report file into.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputFile
    val xmlReportFilePath: Path by lazy {
        extension.reportsPath.resolve("xml").resolve("main.xml")
    }

    /**
     * The directory to generate the xml report file into.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val htmlReportDirectoryPath: Path by lazy {
        extension.reportsPath.resolve("html")
    }

    /**
     * The directory to copy failed tests into.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val failureResultDirectory by lazy {
        extension.reportsPath.resolve("failures") ?: throw IllegalArgumentException("reportPath cannot be null")
    }

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    private var nextResultId = 0L

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
        FilesUtils.deleteRecursivelyIfExisting(extension.reportsPath)
        Files.createDirectories(processedSourcesPath)

        val results = runTests()

        writeXmlReport(results)
        writeHtmlReport(results)
        copyFailures(results)

        val successfulTestAmount = results.count { it.isSuccess }
        val failedTestAmount = results.count { !it.isSuccess }

        println("${results.size} tests ran.\n$successfulTestAmount successful and $failedTestAmount failed.")

        if (failedTestAmount > 0) throw GradleException("Failing tests.")
    }

    private fun runTests(): List<SquitResult> {
        val resultList = arrayListOf<SquitResult>()

        FilesUtils.getSortedLeafDirectories(processedResponsesPath).forEach { actualResponsePath ->
            val propertiesPath = FilesUtils.validateExistence(processedSourcesPath
                    .resolve(actualResponsePath.cut(processedResponsesPath)).resolve(CONFIG))

            val properties = SquitProperties().fillFromSingleProperties(propertiesPath)

            if (shouldReportTest(properties)) {
                val actualResponseFilePath = FilesUtils.validateExistence(actualResponsePath.resolve(ACTUAL_RESPONSE))
                val expectedResponseFilePath = FilesUtils.validateExistence(processedSourcesPath
                        .resolve(actualResponsePath.cut(processedResponsesPath))
                        .resolve(EXPECTED_RESPONSE))

                val expectedResponse = Files.readAllBytes(expectedResponseFilePath)
                val actualResponse = Files.readAllBytes(actualResponseFilePath)

                val diffBuilder = DiffBuilder.compare(Input.fromStream(ByteArrayInputStream(expectedResponse)))
                        .withTest(Input.fromStream(ByteArrayInputStream(actualResponse)))
                        .ignoreWhitespace()
                        .build()

                resultList += constructResult(diffBuilder.differences.joinToString("\n"), actualResponsePath)
            } else {
                logger.warn("Ignoring test ${actualResponsePath.cut(processedResponsesPath)}")
            }
        }

        return resultList
    }

    private fun writeXmlReport(result: List<SquitResult>) {
        Files.createDirectories(xmlReportFilePath.parent)

        val reportFilePath = FilesUtils.createFileIfNotExists(xmlReportFilePath)

        XmlReportWriter.writeReport(result, reportFilePath)
    }

    private fun writeHtmlReport(result: List<SquitResult>) {
        Files.createDirectories(htmlReportDirectoryPath)

        val reportFilePath = FilesUtils.createFileIfNotExists(htmlReportDirectoryPath)

        HtmlReportWriter.writeReport(result, reportFilePath)
    }

    private fun copyFailures(result: List<SquitResult>) {
        FilesUtils.deleteRecursivelyIfExisting(failureResultDirectory)
        Files.createDirectories(failureResultDirectory)

        result.filterNot { it.isSuccess }.forEach {
            val resultDirectoryPath = Files.createDirectories(failureResultDirectory.resolve(it.fullPath))

            val testProcessedSourcesPath = FilesUtils.validateExistence(processedSourcesPath.resolve(it.fullPath))
            val testActualResponsesPath = FilesUtils.validateExistence(processedResponsesPath.resolve(it.fullPath))
            val testDifferenceFile = Files.createFile(resultDirectoryPath.resolve("diff.txt"))

            FilesUtils.copyFilesFromDirectory(testProcessedSourcesPath, resultDirectoryPath)
            FilesUtils.copyFilesFromDirectory(testActualResponsesPath, resultDirectoryPath)
            Files.write(testDifferenceFile, it.result.toByteArray())
        }
    }

    private fun constructResult(differences: String, actualResponsePath: Path): SquitResult {
        val squitBuildDirectoryPath = Paths.get(project.buildDir.path, SQUIT_DIRECTORY)
        val contextPath = actualResponsePath.parent.parent.cut(processedResponsesPath)
        val suitePath = actualResponsePath.parent.fileName
        val testDirectoryPath = actualResponsePath.fileName
        val id = nextResultId++

        return when (differences.isNotBlank()) {
            true -> SquitResult(id, differences, contextPath, suitePath, testDirectoryPath, squitBuildDirectoryPath)
            false -> SquitResult(id, "", contextPath, suitePath, testDirectoryPath, squitBuildDirectoryPath)
        }
    }

    private fun shouldReportTest(properties: SquitProperties) = !properties.ignore
            || project.properties.containsKey("unignore") || project.properties.containsKey("unignoreForReport")
}
