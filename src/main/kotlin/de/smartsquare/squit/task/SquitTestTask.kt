package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Optional
import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.config.shouldIgnore
import de.smartsquare.squit.config.title
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.report.HtmlReportWriter
import de.smartsquare.squit.report.XmlReportWriter
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE_INFO
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.DIFF
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.countTestResults
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.dir
import de.smartsquare.squit.util.file
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * Task for comparing the actual responses to the expected responses and generating a report.
 */
@CacheableTask
abstract class SquitTestTask : DefaultTask() {

    /**
     * The path to save reports and possible failures in.
     */
    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    /**
     * If squit should avoid printing anything if all tests pass.
     */
    @get:Internal
    abstract val silent: Property<Boolean>

    /**
     * If failures should be ignored.
     * In that case the task passes, even if tests have failed.
     */
    @get:Input
    abstract val ignoreFailures: Property<Boolean>

    /**
     * Configuration class for various properties of the media types.
     */
    @get:Nested
    abstract val mediaTypeConfig: Property<MediaTypeConfig>

    /**
     * The directory of the test sources.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val processedSourcesDir = project.layout.buildDirectory.dir(SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The directory of the previously (processed) requested responses.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val processedResponsesDir = project.layout.buildDirectory
        .dir(SQUIT_DIRECTORY, RESPONSES_DIRECTORY, PROCESSED_DIRECTORY)

    /**
     * Collection of meta.json files for up-to-date checking.
     */
    @Suppress("unused")
    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val metaFiles: Provider<FileTree> = project.layout.buildDirectory
        .dir(SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)
        .map { dir -> dir.asFileTree.matching { tree -> tree.include("**/$META") } }

    /**
     * The directory to generate the xml report file into.
     */
    @get:OutputFile
    val xmlReportFile
        get() = reportDir.file("xml", "index.xml")

    /**
     * The directory to generate the xml report file into.
     */
    @get:OutputDirectory
    val htmlReportDir: Provider<Directory>
        get() = reportDir.dir("html")

    /**
     * The directory to copy failed tests into.
     */
    @get:OutputDirectory
    val failureReportDir: Provider<Directory>
        get() = reportDir.dir("failures")

    private var nextResultId = 0L

    init {
        group = "Build"
        description = "Compares the actual responses to the expected responses and generates a report."
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(reportDir.asPath)
        Files.createDirectories(processedSourcesDir.asPath)

        val results = processTests()

        writeXmlReport(results)
        writeHtmlReport(results)
        copyFailures(results)

        val (successfulTests, failedTests, ignoredTests) = results.countTestResults()

        if (!silent.get()) {
            val totalText = if (results.size == 1) "One test ran." else "${results.size} tests ran."
            val ignoredText = if (ignoredTests > 0) " ($ignoredTests ignored)" else ""

            println("$totalText\n$successfulTests successful and $failedTests failed$ignoredText.")
            println()
            println("XML report: file://${xmlReportFile.asPath}")
            println("HTML report: file://${htmlReportDir.asPath.resolve("index.html")}")
        }

        if (failedTests > 0 && !ignoreFailures.get()) throw GradleException("Failing tests.")
    }

    private fun processTests(): List<SquitResult> {
        val resultList = mutableListOf<SquitResult>()

        FilesUtils.getLeafDirectories(processedResponsesDir.asPath).forEach { actualResponsePath ->
            val configPath = FilesUtils.validateExistence(
                processedSourcesDir.asPath
                    .resolve(actualResponsePath.cut(processedResponsesDir.asPath))
                    .resolve(CONFIG),
            )

            val config = ConfigFactory.parseFile(configPath.toFile())
            val expectedResponseInfo = SquitResponseInfo.fromConfig(config)

            if (shouldReportTest(config)) {
                val errorFile = actualResponsePath.resolve(ERROR)

                resultList += if (Files.exists(errorFile)) {
                    constructResult(
                        FilesUtils.readAllBytes(errorFile).toString(Charset.defaultCharset()),
                        expectedResponseInfo,
                        actualResponsePath,
                        config,
                    )
                } else {
                    val bodyDiff = createBodyDifference(actualResponsePath, config)
                    val infoDiff = createResponseInfoDifference(actualResponsePath, expectedResponseInfo)
                    val diff = "$infoDiff$bodyDiff"

                    constructResult(diff, expectedResponseInfo, actualResponsePath, config)
                }
            } else {
                resultList += constructResult("", expectedResponseInfo, actualResponsePath, config, true)
            }
        }

        return resultList
    }

    private fun createResponseInfoDifference(
        actualResponsePath: Path,
        expectedResponseInfo: SquitResponseInfo,
    ): String {
        if (!expectedResponseInfo.isDefault) {
            val contextPath = actualResponsePath.parent.parent.cut(processedResponsesDir.asPath)
            val suitePath = actualResponsePath.parent.fileName
            val path: Path = contextPath.resolve(suitePath)
            val squitBuildDirectoryPath = project.layout.buildDirectory.asPath.resolve(SQUIT_DIRECTORY)
            val testDirectoryPath = actualResponsePath.fileName
            val fullPath = path.resolve(testDirectoryPath)
            val resolvedPath = squitBuildDirectoryPath
                .resolve(RESPONSES_DIRECTORY)
                .resolve(RAW_DIRECTORY)
                .resolve(fullPath)

            val actualResponseInfoPath = FilesUtils.validateExistence(
                resolvedPath.resolve(ACTUAL_RESPONSE_INFO),
            )

            val actualResponse = FilesUtils.readAllBytes(actualResponseInfoPath).toString(Charset.defaultCharset())
            val responseInfo = SquitResponseInfo.fromJson(actualResponse)
            return expectedResponseInfo.diff(responseInfo)
        }

        return ""
    }

    private fun createBodyDifference(actualResponsePath: Path, config: Config): String {
        val actualResponseFilePath = FilesUtils.validateExistence(
            actualResponsePath.resolve(MediaTypeFactory.actualResponse(config.mediaType)),
        )

        val expectedResponseFilePath = FilesUtils.validateExistence(
            processedSourcesDir.asPath
                .resolve(actualResponsePath.cut(processedResponsesDir.asPath))
                .resolve(MediaTypeFactory.expectedResponse(config.mediaType)),
        )

        val expectedResponse = FilesUtils.readAllBytes(expectedResponseFilePath)
        val actualResponse = FilesUtils.readAllBytes(actualResponseFilePath)

        return MediaTypeFactory.differ(config.mediaType, mediaTypeConfig.get())
            .diff(expectedResponse, actualResponse)
    }

    private fun writeXmlReport(result: List<SquitResult>) {
        Files.createDirectories(xmlReportFile.asPath.parent)

        XmlReportWriter().writeReport(result, xmlReportFile.asPath)
    }

    private fun writeHtmlReport(result: List<SquitResult>) {
        Files.createDirectories(htmlReportDir.asPath)

        HtmlReportWriter(logger).writeReport(result, htmlReportDir.asPath, mediaTypeConfig.get())
    }

    private fun copyFailures(result: List<SquitResult>) {
        FilesUtils.deleteRecursivelyIfExisting(failureReportDir.asPath)
        Files.createDirectories(failureReportDir.asPath)

        result.filterNot { it.isSuccess }.forEach {
            val resultDirectoryPath = Files.createDirectories(failureReportDir.asPath.resolve(it.fullPath))

            val testProcessedSourcesPath = FilesUtils
                .validateExistence(processedSourcesDir.asPath.resolve(it.fullPath))

            val testActualResponsesPath = FilesUtils
                .validateExistence(processedResponsesDir.asPath.resolve(it.fullPath))

            val testDifferenceFile = Files.createFile(resultDirectoryPath.resolve(DIFF))

            FilesUtils.copyFilesFromDirectory(testProcessedSourcesPath, resultDirectoryPath)
            FilesUtils.copyFilesFromDirectory(testActualResponsesPath, resultDirectoryPath)
            Files.write(testDifferenceFile, it.difference.toByteArray())
        }
    }

    private fun constructResult(
        differences: String,
        responseInfo: SquitResponseInfo,
        actualResponsePath: Path,
        config: Config,
        isIgnored: Boolean = false,
    ): SquitResult {
        val squitBuildDirectoryPath = project.layout.buildDirectory.dir(SQUIT_DIRECTORY).asPath
        val contextPath = actualResponsePath.parent.parent.cut(processedResponsesDir.asPath)
        val suitePath = actualResponsePath.parent.fileName
        val testDirectoryPath = actualResponsePath.fileName
        val id = nextResultId++

        return when (differences.isNotBlank()) {
            true -> SquitResult(
                id, differences, responseInfo, isIgnored, config.mediaType, config.title,
                contextPath, suitePath,
                testDirectoryPath, squitBuildDirectoryPath,
            )

            false -> SquitResult(
                id, "", responseInfo, isIgnored, config.mediaType, config.title,
                contextPath, suitePath,
                testDirectoryPath, squitBuildDirectoryPath,
            )
        }
    }

    private fun shouldReportTest(config: Config) = !config.shouldIgnore ||
        project.properties.containsKey("unexclude") ||
        project.properties.containsKey("unignore")
}
