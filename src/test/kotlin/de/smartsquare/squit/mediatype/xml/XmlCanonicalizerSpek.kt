package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object XmlCanonicalizerSpek : Spek({
    given("an xml canonicalizer") {
        val canonicalizer = XmlCanonicalizer()

        on("canonicalizing an xml structure") {
            // language=xml
            val structure = """
                <test>
                    <hello b="b" a="a">Abc</hello>
                    <!-- Test -->
                </test>
            """.trimIndent()

            it("should produce a valid result") {
                val result = canonicalizer.canonicalize(
                    structure, MediaTypeConfig(
                        xmlStrict = false,
                        xmlCanonicalize = true,
                        jsonCanonicalize = false
                    )
                )

                // language=xml
                val expected = """
                    <?xml version="1.0" encoding="UTF-8"?>

                    <test>
                      <hello a="a" b="b">Abc</hello>
                    </test>

                """.trimIndent()

                result shouldBeEqualTo expected
            }
        }

        on("canonicalizing an xml structure when canonicalization is disabled") {
            // language=xml
            val structure = """
                <test>
                    <hello b="b" a="a">Abc</hello>
                    <!-- Test -->
                </test>
            """.trimIndent()

            it("should return the input") {
                val result = canonicalizer.canonicalize(
                    structure, MediaTypeConfig(
                        xmlStrict = false,
                        xmlCanonicalize = false,
                        jsonCanonicalize = false
                    )
                )

                result shouldBeEqualTo structure
            }
        }
    }
})
