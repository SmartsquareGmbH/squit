package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.mediatype.MediaTypeFactory.xmlMediaType
import io.mockk.mockk
import net.javacrumbs.jsonunit.JsonAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HtmlReportWriterTest {

    private val writer = HtmlReportWriter(mockk(relaxUnitFun = true))
    private val mediaTypeConfig = MediaTypeConfig(xmlCanonicalize = false)

    // language=json
    private val meta = """{ "date": "2024-01-01T00:00:00Z", "duration": 100 }"""

    // language=xml
    private val expected = "<root><expected/></root>"

    // language=xml
    private val actual = "<root><actual/></root>"

    // language=json
    private val actualResponseInfo = """{ "responseCode": 299 }"""

    @Test
    fun `report contains correct json`(@TempDir tempDir: Path) {
        val testPath = Paths.get("context/suite/test")

        val rawDir = Files.createDirectories(tempDir.resolve("responses/raw").resolve(testPath))
        val processedDir = Files.createDirectories(tempDir.resolve("responses/processed").resolve(testPath))
        val sourcesDir = Files.createDirectories(tempDir.resolve("sources").resolve(testPath))

        Files.writeString(rawDir.resolve("meta.json"), meta)
        Files.writeString(sourcesDir.resolve("description.md"), "description")
        Files.writeString(sourcesDir.resolve(MediaTypeFactory.expectedResponse(xmlMediaType)), expected)
        Files.writeString(processedDir.resolve(MediaTypeFactory.actualResponse(xmlMediaType)), actual)
        Files.writeString(rawDir.resolve("actual_response_info.json"), actualResponseInfo)

        val squitResult = SquitResult(
            id = 1L,
            difference = "difference",
            expectedResponseInfo = SquitResponseInfo(responseCode = 200),
            isIgnored = false,
            mediaType = xmlMediaType,
            alternativeName = "alternative",
            contextPath = Paths.get("context"),
            suitePath = Paths.get("suite"),
            testDirectoryPath = Paths.get("test"),
            squitBuildDirectoryPath = tempDir,
        )

        writer.writeReport(listOf(squitResult), tempDir.resolve("report"), mediaTypeConfig)

        JsonAssert.assertJsonEquals(
            // language=json
            $$"""
            {
                "version": "${json-unit.any-string}",
                "generatedAt": "${json-unit.any-string}",
                "startedAt": "2024-01-01T00:00:00Z",
                "totalDuration": 100,
                "averageDuration": 100,
                "slowestTest": { "id": 1, "name": "test", "duration": 100 },
                "results": {
                    "context": {
                        "suite": {
                            "test": {
                                "id": 1,
                                "alternativeName": "alternative",
                                "description": "description",
                                "success": false,
                                "ignored": false,
                                "error": false,
                                "duration": 100,
                                "expected": "<root><expected/></root>",
                                "actual": "<root><actual/></root>",
                                "infoExpected": "{\n  \"responseCode\": 200\n}",
                                "infoActual": "{ \"responseCode\": 299 }",
                                "language": "xml"
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
            readReportJson(tempDir.resolve("report/index.html")),
        )
    }

    private fun readReportJson(reportPath: Path): String = Files.readString(reportPath)
        .substringAfter("<script type=\"application/json\" id=\"squit-data\">")
        .substringBefore("</script>")
}
