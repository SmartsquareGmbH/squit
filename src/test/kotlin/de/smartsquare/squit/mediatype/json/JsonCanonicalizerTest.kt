package de.smartsquare.squit.mediatype.json

import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object JsonCanonicalizerTest : Spek({
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
                val result = canonicalizer.canonicalize(structure)

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

                result shouldEqual expected
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
                val result = canonicalizer.canonicalize(structure)

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

                result shouldEqual expected
            }
        }

        on("canonicalizing a json structure with different number formats") {
            // language=json
            val structure = """
            {
              "A": 12,
              "B": 0.0,
              "C": 12.0,
              "D": 12.5
            }      
            """.trimIndent()

            it("should produce a sorted, formatted and valid result") {
                val result = canonicalizer.canonicalize(structure)

                // language=json
                val expected = """
                {
                  "A": 12,
                  "B": 0,
                  "C": 12,
                  "D": 12.5
                }
                """.trimIndent()

                result shouldEqual expected
            }
        }
    }
})
