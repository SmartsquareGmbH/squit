package de.smartsquare.squit.io

import de.smartsquare.squit.TestUtils
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.IOException

object JsonParserSupportSpek : Spek({

    val jsonTestProject = TestUtils.getResourcePath("test-project-json")

    val sampleJsonPath = jsonTestProject
        .resolve("src")
        .resolve("squit")
        .resolve("project")
        .resolve("call1")
        .resolve("request.json")

    given("a path to a valid json file") {
        on("reading it") {
            val element = JsonParserSupport.read(sampleJsonPath)

            it("should be read correctly") {
                element.asJsonObject["test"].asInt shouldBe 123
            }
        }
    }

    given("a path to a non-existing json file") {
        val nonExisting = jsonTestProject.resolve("non-existing")

        on("reading it") {
            val readCall = { JsonParserSupport.read(nonExisting) }

            it("should throw a proper exception") {
                val expectedMessage = "Could not read json file: $nonExisting"

                readCall shouldThrow GradleException::class withMessage expectedMessage withCause IOException::class
            }
        }
    }
})
