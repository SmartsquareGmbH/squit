package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.SquitExtension
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Ruben Gees
 */
object XmlDifferSpek : Spek({

    given("a strict XmlDiffer") {
        val extension = SquitExtension(ProjectBuilder.builder().build()).also { it.xml.strict = true }
        val differ = XmlDiffer(extension)

        on("diffing identic XMLs") {
            it("should return an empty String, signaling no differences") {
                differ.diff("<cool/>".toByteArray(), "<cool/>".toByteArray()).shouldBeEmpty()
            }
        }

        on("diffing similar XMLs with only differences in namespace prefixes") {
            it("should return a String, containing the differences") {
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

                differ.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
            }
        }

        on("diffing different XMLs") {
            it("should return a String, containing the differences") {
                val expected = "<good/>"
                val actual = "<bad/>"

                differ.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
            }
        }
    }

    given("a non strict XmlDiffer") {
        val extension = SquitExtension(ProjectBuilder.builder().build()).also { it.xml.strict = false }
        val differ = XmlDiffer(extension)

        on("diffing identic XMLs") {
            it("should return an empty String, signaling no differences") {
                differ.diff("<cool/>".toByteArray(), "<cool/>".toByteArray()).shouldBeEmpty()
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

                differ.diff(expected.toByteArray(), actual.toByteArray()).shouldBeEmpty()
            }
        }

        on("diffing different XMLs") {
            it("should return a String, containing the differences") {
                val expected = "<good/>"
                val actual = "<bad/>"

                differ.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
            }
        }
    }
})