package de.smartsquare.squit.report

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.amshove.kluent.`should be equal to`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Sascha Koch
 */
object HtmlReportWriterSpek : Spek({

    val writer = HtmlReportWriter

    given("a squit result with default expected response code") {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo()
        every { result.mediaType } returns "application/plain".toMediaTypeOrNull()!!

        on("unifiying this to js text") {
            it("should have an empty diff") {
                val extension = mockk<SquitExtension> {
                    every { json } returns SquitExtension.JsonExtension()
                }

                writer.prepareInfoForJs(result, extension) `should be equal to` ""
            }
        }
    }

    given("a squit result with expected response code and no actual info file") {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns emptyList()
        every { result.mediaType } returns "application/plain".toMediaTypeOrNull()!!

        on("unifiying this to js text") {
            it("should have a correct diff") {
                val extension = mockk<SquitExtension> {
                    every { json } returns SquitExtension.JsonExtension()
                }

                writer.prepareInfoForJs(result, extension) `should be equal to` "--- ResultInfo\\n\\\n" +
                    "+++ ResultInfo\\n\\\n" +
                    "@@ -1,3 +1,0 @@\\n\\\n" +
                    "-{\\n\\\n-  \\\"responseCode\\\": 200\\n\\\n-}"
            }
        }
    }

    given("a squit result with expected response code and an actual info file") {
        val result = mockk<SquitResult>()

        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns listOf("{", "\"responseCode\":200", "}")
        every { result.mediaType } returns "application/plain".toMediaTypeOrNull()!!

        on("unifiying this to js text") {
            it("should have no diff with the response code") {
                val extension = mockk<SquitExtension> {
                    every { json } returns SquitExtension.JsonExtension()
                }

                writer.prepareInfoForJs(result, extension) `should be equal to` "--- Result\\n\\\n" +
                    "+++ Result\\n\\\n" +
                    "@@ -1 +1 @@\\n\\\n" +
                    " {\\n\\\n   \\\"responseCode\\\": 200\\n\\\n }"
            }
        }
    }
})
