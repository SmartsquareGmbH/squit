package de.smartsquare.squit.report

import com.google.gson.Gson
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.mediatype.MediaTypeFactory.xmlMediaType
import io.mockk.mockk
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HtmlReportWriterTest {

    private val writer = HtmlReportWriter(mockk(relaxUnitFun = true))
    private val mediaTypeConfig = MediaTypeConfig()

    @TempDir
    private lateinit var tempDir: Path

    private lateinit var squitResult: SquitResult

    @BeforeEach
    fun setUp() {
        val testDir = Paths.get("test")

        val rawDir = Files.createDirectories(tempDir.resolve("responses/raw").resolve(testDir))
        val processedDir = Files.createDirectories(tempDir.resolve("responses/processed").resolve(testDir))
        val sourcesDir = Files.createDirectories(tempDir.resolve("sources").resolve(testDir))

        Files.writeString(rawDir.resolve("meta.json"), """{"date":"2024-01-01T00:00:00","duration":100}""")
        Files.writeString(sourcesDir.resolve(MediaTypeFactory.expectedResponse(xmlMediaType)), "<test></test>")
        Files.writeString(processedDir.resolve(MediaTypeFactory.actualResponse(xmlMediaType)), "<test></test>")

        squitResult = SquitResult(
            id = 1L,
            difference = "",
            expectedResponseInfo = SquitResponseInfo(),
            isIgnored = false,
            mediaType = xmlMediaType,
            alternativeName = "",
            contextPath = Paths.get(""),
            suitePath = Paths.get(""),
            testDirectoryPath = testDir,
            squitBuildDirectoryPath = tempDir,
        )
    }

    @Test
    fun `report index html is written and contains injected json`() {
        writer.writeReport(listOf(squitResult), tempDir.resolve("report"), mediaTypeConfig)

        Files.exists(tempDir.resolve("report/index.html")).shouldBeTrue()

        val reportData = readReport(tempDir.resolve("report/index.html"))

        reportData.slowestTest.shouldNotBeNull()
        reportData.results.shouldNotBeEmpty()
    }

    private fun readReport(reportPath: Path): SquitHtmlReportData {
        val json = Files.readString(reportPath)
            .substringAfter("<script type=\"application/json\" id=\"squit-data\">")
            .substringBefore("</script>")

        return Gson().fromJson(json, SquitHtmlReportData::class.java)
    }
}
