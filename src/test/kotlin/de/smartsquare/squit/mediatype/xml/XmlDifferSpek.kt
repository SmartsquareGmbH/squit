package de.smartsquare.squit.mediatype.xml

import org.amshove.kluent.shouldBeEmpty
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Ruben Gees
 */
object XmlDifferSpek : Spek({

    given("a XmlDiffer") {
        on("diffing identic XMLs") {
            it("should return an empty String, signaling no differences") {
                XmlDiffer.diff("<cool/>".toByteArray(), "<cool/>".toByteArray()).shouldBeEmpty()
            }
        }

        on("diffing similar XMLs with one having an extra unused namespace") {
            it("should return an empty String, signaling no differences") {
                val expected = """
                    <root xmlns:ns="http://example.com">
                      <cool/>
                    </root>
                """.trimIndent()

                val actual = """
                    <root>
                      <cool/>
                    </root>
                """.trimIndent()

                XmlDiffer.diff(expected.toByteArray(), actual.toByteArray()).shouldBeEmpty()
            }
        }

        on("diffing similar XMLs with only differences in namespace prefixes") {
            it("should return an empty String, signaling no differences") {
                val expected = """
                    <ns:root xmlns:ns="http://example.com">
                      <cool/>
                    </ns:root>
                """.trimIndent()

                val actual = """
                    <something:root xmlns:something="http://example.com">
                      <cool/>
                    </something:root>
                """.trimIndent()

                XmlDiffer.diff(expected.toByteArray(), actual.toByteArray()).shouldBeEmpty()
            }
        }
    }
})
