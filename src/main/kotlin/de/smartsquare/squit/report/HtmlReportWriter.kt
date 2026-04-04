package de.smartsquare.squit.report

import com.google.gson.Gson
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import okhttp3.MediaType
import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path

/**
 * Object for writing the Squit HTML report.
 */
class HtmlReportWriter(private val logger: Logger) {

    private companion object {
        private const val DATA_PLACEHOLDER = "__SQUIT_DATA__"
    }

    /**
     * Generates and writes the Squit HTML report, given the [results] list and [reportDirectoryPath].
     */
    fun writeReport(results: List<SquitResult>, reportDirectoryPath: Path, mediaTypeConfig: MediaTypeConfig) {
        val firstTest = results.minByOrNull { it.metaInfo.date }
        val duration = results.fold(0L) { acc, next -> acc + next.metaInfo.duration }

        val averageTime = results.map { it.metaInfo.duration }.average().let { if (it.isNaN()) 0L else it.toLong() }
        val slowestTest = results.maxByOrNull { it.metaInfo.duration }

        val data = SquitHtmlReportData(
            startedAt = firstTest?.metaInfo?.date?.toString(),
            totalDuration = duration,
            averageDuration = averageTime,
            slowestTest = slowestTest?.let { SquitSlowestTest(it.id, it.simpleName, it.metaInfo.duration) },
            results = constructResults(results, mediaTypeConfig),
        )

        val json = Gson().toJson(data)

        val template = javaClass.getResourceAsStream("/squit-report-template.html")!!
            .readBytes()
            .toString(Charsets.UTF_8)

        val html = template.replace(DATA_PLACEHOLDER, json)

        Files.createDirectories(reportDirectoryPath)
        Files.write(reportDirectoryPath.resolve("index.html"), html.toByteArray())
    }

    private fun constructResults(results: List<SquitResult>, mediaTypeConfig: MediaTypeConfig): Map<String, Any> {
        val root = SquitReportResultBranch()

        for (result in results) {
            var current = root

            for (segment in result.path) {
                val next = current.children.getOrPut(segment.toString()) { SquitReportResultBranch() }

                current = next as? SquitReportResultBranch
                    ?: error("Path segment '$segment' is already occupied by a result.")
            }

            current.children[result.simpleName] = buildReportResult(result, mediaTypeConfig)
        }

        return root.toMap()
    }

    private fun buildReportResult(result: SquitResult, mediaTypeConfig: MediaTypeConfig): SquitReportResult {
        val canonicalizedExpectedLines = canonicalizeExpectedLines(result, mediaTypeConfig)
        val canonicalizedActualLines = canonicalizeActualLines(result, mediaTypeConfig)

        val hasInfoDiff = !result.expectedResponseInfo.isDefault && !result.isError
        val infoExpected = if (hasInfoDiff) result.expectedResponseInfo.toJson() else null
        val infoActual = if (hasInfoDiff) result.actualLines.joinToString("\n") else null

        return SquitReportResult(
            result.id,
            result.alternativeName,
            result.description,
            result.isSuccess,
            result.isIgnored,
            result.isError,
            result.metaInfo.duration,
            canonicalizedExpectedLines.joinToString("\n"),
            canonicalizedActualLines.joinToString("\n"),
            infoExpected,
            infoActual,
            highlightLanguage(result.mediaType),
        )
    }

    private fun highlightLanguage(mediaType: MediaType): String? = when (mediaType) {
        MediaTypeFactory.xmlMediaType, MediaTypeFactory.applicationXmlMediaType, MediaTypeFactory.soapMediaType -> "xml"
        MediaTypeFactory.jsonMediaType -> "json"
        else -> null
    }

    private fun canonicalizeExpectedLines(result: SquitResult, mediaTypeConfig: MediaTypeConfig): List<String> =
        if (!result.isError) {
            canonicalize(
                result.expectedLines,
                result.mediaType,
                mediaTypeConfig,
                "Could not canonicalize expected response",
            )
        } else {
            result.expectedLines
        }

    private fun canonicalizeActualLines(result: SquitResult, mediaTypeConfig: MediaTypeConfig): List<String> =
        if (!result.isError) {
            canonicalize(
                result.actualLines,
                result.mediaType,
                mediaTypeConfig,
                "Could not canonicalize actual response",
            )
        } else {
            result.actualLines
        }

    private fun canonicalize(
        lines: List<String>,
        mediaType: MediaType,
        mediaTypeConfig: MediaTypeConfig,
        errorMessage: String,
    ): List<String> = when {
        lines.isEmpty() -> lines

        else -> try {
            MediaTypeFactory.canonicalizer(mediaType)
                .canonicalize(lines.joinToString(""), mediaTypeConfig)
                .lines()
        } catch (error: Exception) {
            logger.warn(errorMessage, error)

            lines
        }
    }
}
