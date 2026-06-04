package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.mediatype.MediaTypeFactory.xmlMediaType
import de.smartsquare.squit.util.gson
import io.mockk.mockk
import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.core.Option
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SquitReportResultBranchAdapterTest {

    private val adapter =
        SquitReportResultBranchAdapter(MediaTypeConfig(xmlCanonicalize = false), mockk(relaxUnitFun = true))

    private val reportGson = gson.newBuilder()
        .registerTypeAdapter(SquitReportResultBranch::class.java, adapter)
        .create()

    @TempDir
    private lateinit var tempDir: Path

    private lateinit var result: SquitResult

    @BeforeEach
    fun setUp() {
        val testPath = Paths.get("ctx/suite/test")

        val rawDir = Files.createDirectories(tempDir.resolve("responses/raw").resolve(testPath))
        val processedDir = Files.createDirectories(tempDir.resolve("responses/processed").resolve(testPath))
        val sourcesDir = Files.createDirectories(tempDir.resolve("sources").resolve(testPath))

        Files.writeString(rawDir.resolve("meta.json"), """{"date":"2024-01-01T00:00:00Z","duration":0}""")
        Files.writeString(sourcesDir.resolve(MediaTypeFactory.expectedResponse(xmlMediaType)), "<expected/>")
        Files.writeString(processedDir.resolve(MediaTypeFactory.actualResponse(xmlMediaType)), "<actual/>")

        result = SquitResult(
            id = 1L,
            difference = "",
            expectedResponseInfo = SquitResponseInfo(),
            isIgnored = false,
            mediaType = xmlMediaType,
            alternativeName = "",
            contextPath = Paths.get("ctx"),
            suitePath = Paths.get("suite"),
            testDirectoryPath = Paths.get("test"),
            squitBuildDirectoryPath = tempDir,
        )
    }

    @Test
    fun `empty branch writes empty object`() {
        JsonAssert.assertJsonEquals("{}", reportGson.toJson(SquitReportResultBranch()))
    }

    @Test
    fun `flat branch with single leaf writes key and result`() {
        val branch = SquitReportResultBranch(mutableMapOf("test" to SquitReportResultLeaf(result)))

        JsonAssert.assertJsonEquals(
            // language=json
            """{ "test": { "id": 1, "expected": "<expected/>", "actual": "<actual/>" }}""",
            reportGson.toJson(branch),
            JsonAssert.`when`(Option.IGNORING_EXTRA_FIELDS),
        )
    }

    @Test
    fun `nested branch writes intermediate keys`() {
        val branch = SquitReportResultBranch(
            mutableMapOf(
                "ctx" to SquitReportResultBranch(
                    mutableMapOf(
                        "suite" to SquitReportResultBranch(
                            mutableMapOf("test" to SquitReportResultLeaf(result)),
                        ),
                    ),
                ),
            ),
        )

        JsonAssert.assertJsonEquals(
            // language=json
            """{ "ctx": { "suite": { "test": { "id": 1 }}}}""",
            reportGson.toJson(branch),
            JsonAssert.`when`(Option.IGNORING_EXTRA_FIELDS),
        )
    }

    @Test
    fun `branch with multiple leaves writes all keys`() {
        val branch = SquitReportResultBranch(
            mutableMapOf(
                "a" to SquitReportResultLeaf(result),
                "b" to SquitReportResultLeaf(result),
            ),
        )

        JsonAssert.assertJsonEquals(
            // language=json
            """{ "a": { "id": 1 }, "b": { "id": 1 }}""",
            reportGson.toJson(branch),
            JsonAssert.`when`(Option.IGNORING_EXTRA_FIELDS),
        )
    }
}
