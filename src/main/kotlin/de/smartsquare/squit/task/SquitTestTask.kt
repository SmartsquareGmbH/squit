package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Optional
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.report.HtmlReportWriter
import de.smartsquare.squit.report.XmlReportWriter
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.DIFF
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.mediaType
import de.smartsquare.squit.util.shouldIgnore
import de.smartsquare.squit.util.title
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates
import kotlin.streams.toList

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
    @Suppress("MemberVisibilityCanBePrivate")
    @get:InputDirectory
    val processedSourcesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY, SOURCES_DIRECTORY
    )

    /**
     * The directory of the previously (processed) requested responses.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:InputDirectory
    val processedResponsesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY, RESPONSES_DIRECTORY, PROCESSED_DIRECTORY
    )

    /**
     * Collection of meta.json files for up-to-date checking.
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    @get:InputFiles
    @get:Optional
    val metaPaths: List<Path> by lazy {
        val rawDirectoryPath = Paths.get(project.buildDir.path, SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)

        if (Files.exists(rawDirectoryPath)) {
            Files.walk(rawDirectoryPath).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString() == META }.toList()
            }
        } else {
            emptyList<Path>()
        }
    }

    /**
     * The directory to generate the xml report file into.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputFile
    val xmlReportFilePath: Path by lazy {
        extension.reportsPath.resolve("xml").resolve("index.xml")
    }

    /**
     * The directory to generate the xml report file into.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputDirectory
    val htmlReportDirectoryPath: Path by lazy {
        extension.reportsPath.resolve("html")
    }

    /**
     * The directory to copy failed tests into.
     */
    @Suppress("MemberVisibilityCanBePrivate")
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

        val successfulAmount = results.count { !it.isIgnored && it.isSuccess }
        val failedAmount = results.count { !it.isIgnored && !it.isSuccess }
        val ignoredAmount = results.count { it.isIgnored }

        val totalText = if (results.size == 1) "One test ran." else "${results.size} tests ran."
        val ignoredText = if (ignoredAmount > 0) " ($ignoredAmount ignored)" else ""

        println("$totalText\n$successfulAmount successful and $failedAmount failed$ignoredText.")

        if (failedAmount > 0) throw GradleException("Failing tests.")
    }

    private fun runTests(): List<SquitResult> {
        val resultList = arrayListOf<SquitResult>()

        FilesUtils.getSortedLeafDirectories(processedResponsesPath).forEach { actualResponsePath ->
            val configPath = FilesUtils.validateExistence(
                processedSourcesPath
                    .resolve(actualResponsePath.cut(processedResponsesPath)).resolve(CONFIG)
            )

            val config = ConfigFactory.parseFile(configPath.toFile())

            if (shouldReportTest(config)) {
                val errorFile = actualResponsePath.resolve(ERROR)

                if (Files.exists(errorFile)) {
                    resultList += constructResult(
                        Files.readAllBytes(errorFile).toString(Charset.defaultCharset()),
                        actualResponsePath, config
                    )
                } else {
                    val actualResponseFilePath = FilesUtils.validateExistence(
                        actualResponsePath
                            .resolve(MediaTypeFactory.actualResponse(config.mediaType))
                    )

                    val expectedResponseFilePath = FilesUtils.validateExistence(
                        processedSourcesPath
                            .resolve(actualResponsePath.cut(processedResponsesPath))
                            .resolve(MediaTypeFactory.expectedResponse(config.mediaType))
                    )

                    val expectedResponse = Files.readAllBytes(expectedResponseFilePath)
                    val actualResponse = Files.readAllBytes(actualResponseFilePath)

                    val diff = MediaTypeFactory.differ(config.mediaType, extension)
                        .diff(expectedResponse, actualResponse)

                    resultList += constructResult(diff, actualResponsePath, config)
                }
            } else {
                resultList += constructResult("", actualResponsePath, config, true)
            }
        }

        return resultList
    }

    private fun writeXmlReport(result: List<SquitResult>) {
        Files.createDirectories(xmlReportFilePath.parent)

        XmlReportWriter.writeReport(result, xmlReportFilePath)
    }

    private fun writeHtmlReport(result: List<SquitResult>) {
        Files.createDirectories(htmlReportDirectoryPath)

        HtmlReportWriter.writeReport(result, htmlReportDirectoryPath)
    }

    private fun copyFailures(result: List<SquitResult>) {
        FilesUtils.deleteRecursivelyIfExisting(failureResultDirectory)
        Files.createDirectories(failureResultDirectory)

        result.filterNot { it.isSuccess }.forEach {
            val resultDirectoryPath = Files.createDirectories(failureResultDirectory.resolve(it.fullPath))

            val testProcessedSourcesPath = FilesUtils.validateExistence(processedSourcesPath.resolve(it.fullPath))
            val testActualResponsesPath = FilesUtils.validateExistence(processedResponsesPath.resolve(it.fullPath))
            val testDifferenceFile = Files.createFile(resultDirectoryPath.resolve(DIFF))

            FilesUtils.copyFilesFromDirectory(testProcessedSourcesPath, resultDirectoryPath)
            FilesUtils.copyFilesFromDirectory(testActualResponsesPath, resultDirectoryPath)
            Files.write(testDifferenceFile, it.result.toByteArray())
        }
    }

    private fun constructResult(
        differences: String,
        actualResponsePath: Path,
        config: Config,
        isIgnored: Boolean = false
    ): SquitResult {
        val squitBuildDirectoryPath = Paths.get(project.buildDir.path, SQUIT_DIRECTORY)
        val contextPath = actualResponsePath.parent.parent.cut(processedResponsesPath)
        val suitePath = actualResponsePath.parent.fileName
        val testDirectoryPath = actualResponsePath.fileName
        val id = nextResultId++

        return when (differences.isNotBlank()) {
            true -> SquitResult(
                id, differences, isIgnored, config.mediaType, config.title, contextPath, suitePath,
                testDirectoryPath, squitBuildDirectoryPath
            )

            false -> SquitResult(
                id, "", isIgnored, config.mediaType, config.title, contextPath, suitePath,
                testDirectoryPath, squitBuildDirectoryPath
            )
        }
    }

    private fun shouldReportTest(config: Config) = !config.shouldIgnore ||
        project.properties.containsKey("unexclude") ||
        project.properties.containsKey("unignore")
}
