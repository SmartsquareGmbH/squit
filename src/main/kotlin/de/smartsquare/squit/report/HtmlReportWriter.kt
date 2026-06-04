package de.smartsquare.squit.report

import com.google.gson.stream.JsonWriter
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.util.gson
import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties

/**
 * Object for writing the Squit HTML report.
 */
class HtmlReportWriter(private val logger: Logger) {

    private companion object {
        private const val DATA_PLACEHOLDER = "__SQUIT_DATA__"

        private val squitVersion: String by lazy {
            val resource = requireNotNull(javaClass.getResourceAsStream("/squit-version.properties")) {
                "Could not find squit-report-template.html on classpath"
            }

            val props = resource.use { Properties().apply { load(it) } }

            requireNotNull(props.getProperty("version")) { "Missing version property in squit-version.properties" }
        }
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
            version = squitVersion,
            generatedAt = Instant.now().toString(),
            startedAt = firstTest?.metaInfo?.date?.toString(),
            totalDuration = duration,
            averageDuration = averageTime,
            slowestTest = slowestTest?.let { SquitSlowestTest(it.id, it.simpleName, it.metaInfo.duration) },
            results = constructResults(results),
        )

        val reportGson = gson.newBuilder()
            .registerTypeAdapter(
                SquitReportResultBranch::class.java,
                SquitReportResultBranchAdapter(mediaTypeConfig, logger),
            )
            .create()

        val templateContent = requireNotNull(javaClass.getResource("/squit-report-template.html")) {
            "Could not find squit-report-template.html on classpath"
        }.readText()

        val placeholderIndex = templateContent.indexOf(DATA_PLACEHOLDER)
        require(placeholderIndex != -1) { "Could not find $DATA_PLACEHOLDER in squit-report-template.html" }

        Files.createDirectories(reportDirectoryPath)
        Files.newBufferedWriter(reportDirectoryPath.resolve("index.html")).use { writer ->
            writer.write(templateContent, 0, placeholderIndex)
            reportGson.toJson(data, SquitHtmlReportData::class.java, JsonWriter(writer))
            writer.write(
                templateContent,
                placeholderIndex + DATA_PLACEHOLDER.length,
                templateContent.length - placeholderIndex - DATA_PLACEHOLDER.length,
            )
        }
    }

    private fun constructResults(results: List<SquitResult>): SquitReportResultBranch {
        val root = SquitReportResultBranch()

        for (result in results) {
            var current = root

            for (segment in result.path) {
                val key = segment.toString()
                val existing = current.children[key]

                current = when (existing) {
                    null -> SquitReportResultBranch().also { current.children[key] = it }
                    is SquitReportResultBranch -> existing
                    is SquitReportResultLeaf -> error("Path segment '$key' is already occupied by a result.")
                }
            }

            current.children[result.simpleName] = SquitReportResultLeaf(result)
        }

        return root
    }
}
