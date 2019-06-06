package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import io.mockk.every
import io.mockk.mockk
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

        on("unifiying this to js text") {
            it("should have an empty diff") {
                writer.prepareInfoForJs(result) `should be equal to` ""
            }
        }
    }

    given("a squit result with expected response code and no actual info file") {
        val result = mockk<SquitResult>()
        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns emptyList()

        on("unifiying this to js text") {
            it("should have a correct diff") {
                writer.prepareInfoForJs(result) `should be equal to` "--- ResultInfo\\n\\\n" +
                    "+++ ResultInfo\\n\\\n" +
                    "@@ -1,1 +1,0 @@\\n\\\n" +
                    "-{\\\"responseCode\\\":200}"
            }
        }
    }

    given("a squit result with expected response code and an actual info file") {
        val result = mockk<SquitResult>()
        every { result.expectedResponseInfo } returns SquitResponseInfo(200)
        every { result.actualInfoLines } returns listOf("{", "\"responseCode\":200", "}")

        on("unifiying this to js text") {
            it("should have no diff with the response code") {
                writer.prepareInfoForJs(result) `should be equal to` "--- Result\\n\\\n" +
                    "+++ Result\\n\\\n" +
                    "@@ -1 +1 @@\\n\\\n" +
                    " {\\\"responseCode\\\":200}"
            }
        }
    }
})
