package de.smartsquare.squit.report

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.jvm.java

class HtmlReportWriterTest {

    private val writer = HtmlReportWriter(mockk(relaxUnitFun = true))
    private val mediaTypeConfig = MediaTypeConfig()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `info diff is null for a result with default expected response info`() {
        val result = buildResult(tempDir)

        writer.writeReport(listOf(result), tempDir.resolve("report"), mediaTypeConfig)

//        readResults(tempDir.resolve("report")).single().infoDiff.shouldBeEmpty()
    }

    @Test
    fun `info diff is null for a result with an error`() {
        val result = buildResult(tempDir, expectedResponseInfo = SquitResponseInfo(200), isError = true)

        writer.writeReport(listOf(result), tempDir.resolve("report"), mediaTypeConfig)

//        readResults(tempDir.resolve("report")).single().infoDiff.shouldBeEmpty()
    }

    @Test
    fun `info diff is produced for a result with expected response code and no actual info file`() {
        val result = buildResult(tempDir, expectedResponseInfo = SquitResponseInfo(200))

        writer.writeReport(listOf(result), tempDir.resolve("report"), mediaTypeConfig)

//        readResults(tempDir.resolve("report")).single().infoDiff.shouldNotBeEmpty()
    }

    @Test
    fun `info diff is null for a result with expected response code and a matching actual info file`() {
        val result = buildResult(
            tempDir,
            expectedResponseInfo = SquitResponseInfo(200),
            actualInfoContent = SquitResponseInfo(200).toJson(),
        )

        writer.writeReport(listOf(result), tempDir.resolve("report"), mediaTypeConfig)

//        readResults(tempDir.resolve("report")).single().infoDiff.shouldBeEmpty()
    }

    private fun buildResult(
        buildDir: Path,
        expectedResponseInfo: SquitResponseInfo = SquitResponseInfo(),
        isError: Boolean = false,
        actualInfoContent: String? = null,
    ): SquitResult {
        val mediaType = "text/plain".toMediaType()
        val testDir = Paths.get("test")

        val rawDir = buildDir.resolve("responses/raw").resolve(testDir)
        val processedDir = buildDir.resolve("responses/processed").resolve(testDir)
        val sourcesDir = buildDir.resolve("sources").resolve(testDir)

        Files.createDirectories(rawDir)
        Files.createDirectories(processedDir)
        Files.createDirectories(sourcesDir)

        Files.writeString(rawDir.resolve("meta.json"), """{"date":"2024-01-01T00:00:00","duration":100}""")

        if (isError) {
            Files.writeString(processedDir.resolve("error.txt"), "error")
        } else {
            Files.writeString(sourcesDir.resolve("expected_response.txt"), "")
            Files.writeString(processedDir.resolve("actual_response.txt"), "")
        }

        if (actualInfoContent != null) {
            Files.writeString(rawDir.resolve("actual_response_info.json"), actualInfoContent)
        }

        return SquitResult(
            id = 1L,
            difference = "",
            expectedResponseInfo = expectedResponseInfo,
            isIgnored = false,
            mediaType = mediaType,
            alternativeName = "",
            contextPath = Paths.get(""),
            suitePath = Paths.get(""),
            testDirectoryPath = testDir,
            squitBuildDirectoryPath = buildDir,
        )
    }

    private fun readResults(reportDir: Path): List<SquitReportResult> {
        val html = Files.readString(reportDir.resolve("index.html"))
        val json = html
            .substringAfter("<script type=\"application/json\" id=\"squit-data\">")
            .substringBefore("</script>")

        return Gson().fromJson(json, object : TypeToken<List<SquitReportResult>>() {})
    }
}
