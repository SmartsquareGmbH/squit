package de.smartsquare.squit.mediatype.json

import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object JsonCanonicalizerSpek : Spek({
    given("a json canonicalizer") {
        val canonicalizer = JsonCanonicalizer()

        on("canonicalizing a json structure") {
            // language=json
            val structure = """
                {
                  "B": {
                    "Test": {
                    "Test3": "Hello3",
                    "Test2":   "Hello2",
                    "Test4":4
                    }
                  },
                  "Abc": {
                    "Cba": [ 1,3,5,   2 ]
                  },
                  "Y": "Y"
                }
            """.trimIndent()

            it("should produce a sorted, formatted and valid result") {
                val result = canonicalizer.canonicalize(
                    structure,
                    MediaTypeConfig(
                        xmlStrict = false,
                        xmlCanonicalize = false,
                        jsonCanonicalize = true
                    )
                )

                // language=json
                val expected = """
                    {
                      "Abc": {
                        "Cba": [
                          1,
                          3,
                          5,
                          2
                        ]
                      },
                      "B": {
                        "Test": {
                          "Test2": "Hello2",
                          "Test3": "Hello3",
                          "Test4": 4
                        }
                      },
                      "Y": "Y"
                    }
                """.trimIndent()

                result shouldBeEqualTo expected
            }
        }

        on("canonicalizing a json structure with nesting in arrays") {
            // language=json
            val structure = """
                [
                  {
                    "B": "B",
                    "A": "A",
                    "C": "C"
                  },
                  {
                    "A": "A"
                  },
                  {
                    "Y": "Y",
                    "Z": "Z"
                  }
                ]
            """.trimIndent()

            it("should produce a sorted, formatted and valid result") {
                val result = canonicalizer.canonicalize(
                    structure,
                    MediaTypeConfig(
                        xmlStrict = false,
                        xmlCanonicalize = false,
                        jsonCanonicalize = true
                    )
                )

                // language=json
                val expected = """
                    [
                      {
                        "A": "A",
                        "B": "B",
                        "C": "C"
                      },
                      {
                        "A": "A"
                      },
                      {
                        "Y": "Y",
                        "Z": "Z"
                      }
                    ]
                """.trimIndent()

                result shouldBeEqualTo expected
            }
        }

        on("canonicalizing a json structure when canonicalization is disabled") {
            // language=json
            val structure = """
                {
                  "B": {
                    "Test": {
                    "Test3": "Hello3",
                    "Test2":   "Hello2",
                    "Test4":4
                    }
                  },
                  "Abc": {
                    "Cba": [ 1,3,5,   2 ]
                  },
                  "Y": "Y"
                }
            """.trimIndent()

            it("should return the input") {
                val result = canonicalizer.canonicalize(
                    structure,
                    MediaTypeConfig(
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
