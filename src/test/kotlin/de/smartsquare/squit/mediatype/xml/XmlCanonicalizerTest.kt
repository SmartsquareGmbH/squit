package de.smartsquare.squit.mediatype.xml

import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object XmlCanonicalizerTest : Spek({
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
                val result = canonicalizer.canonicalize(structure)

                // language=xml
                val expected = """
                    <?xml version="1.0" encoding="UTF-8"?>

                    <test> 
                      <hello a="a" b="b">Abc</hello> 
                    </test>
                    
                """.trimIndent()

                result shouldEqual expected
            }
        }
    }
})
