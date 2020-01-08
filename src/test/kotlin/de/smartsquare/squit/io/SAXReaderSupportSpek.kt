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

object SAXReaderSupportSpek : Spek({

    val testProject = TestUtils.getResourcePath("test-project")

    val sampleXmlPath = testProject
        .resolve("src")
        .resolve("squit")
        .resolve("project")
        .resolve("call1")
        .resolve("request.xml")

    given("a path to a valid xml file") {
        on("reading it") {
            val document = SAXReaderSupport.read(sampleXmlPath)

            it("should be read correctly") {
                document.selectNodes("//animal").size shouldBe 2
            }
        }
    }

    given("a path to a non-existing xml file") {
        val nonExisting = testProject.resolve("non-existing")

        on("reading it") {
            val readCall = { SAXReaderSupport.read(nonExisting) }

            it("should throw a proper exception") {
                val expectedMessage = "Could not read xml file: $nonExisting"

                readCall shouldThrow GradleException::class withMessage expectedMessage withCause IOException::class
            }
        }
    }
})
