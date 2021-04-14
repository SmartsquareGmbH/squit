package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class HtmlReportWriterTest {

    private val writer = HtmlReportWriter(mockk(relaxUnitFun = true))

    @Test
    fun `preparing a squit result with default expected response code for js`() {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo()
        every { result.mediaType } returns "application/plain".toMediaType()
        every { result.isError } returns false

        writer.prepareInfoForJs(result) shouldBeEqualTo ""
    }

    @Test
    fun `preparing a squit result with an error for js`() {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo()
        every { result.mediaType } returns "application/plain".toMediaType()
        every { result.isError } returns true

        writer.prepareInfoForJs(result) shouldBeEqualTo ""
    }

    @Test
    fun `preparing a squit result with expected response code and no actual info file for js`() {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns emptyList()
        every { result.mediaType } returns "application/plain".toMediaType()
        every { result.isError } returns false

        writer.prepareInfoForJs(result) shouldBeEqualTo """
            --- ResultInfo\n\
            +++ ResultInfo\n\
            @@ -1,3 +1,0 @@\n\
            -{\n\
            -  \"responseCode\": 200\n\
            -}
        """.trimIndent()
    }

    @Test
    fun `preparing a squit result with expected response code and an actual info file for js`() {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns listOf("{", "  \"responseCode\": 200", "}")
        every { result.mediaType } returns "application/plain".toMediaType()
        every { result.isError } returns false

        writer.prepareInfoForJs(result) shouldBeEqualTo """
            --- Result\n\
            +++ Result\n\
            @@ -1 +1 @@\n\
             {\n\
               \"responseCode\": 200\n\
             }
        """.trimIndent()
    }
}
