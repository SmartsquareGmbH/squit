package de.smartsquare.squit.util

import com.google.gson.JsonParser
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.dom4j.io.SAXReader
import org.gradle.api.GradleException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Paths

/**
 * @author Ruben Gees
 */
object UtilExtensionsSpek : Spek({

    val testProject = Paths.get(this.javaClass.classLoader.getResource("test-project").toURI())
    val jsonTestProject = Paths.get(this.javaClass.classLoader.getResource("test-project-json").toURI())

    val sampleXmlPath = testProject
        .resolve("src")
        .resolve("test")
        .resolve("project")
        .resolve("call1")
        .resolve("request.xml")

    val sampleJsonPath = jsonTestProject
        .resolve("src")
        .resolve("test")
        .resolve("project")
        .resolve("call1")
        .resolve("request.json")

    given("two paths") {
        val first = Paths.get("a/b/c/d/e")
        val second = Paths.get("a/b/c/d/g/x")

        on("invoking the cut function") {
            val result = first.cut(second)

            it("should return a correctly mutated path") {
                result shouldEqual Paths.get("e")
            }
        }
    }

    given("two empty paths") {
        val path = Paths.get("")

        on("invoking the cut function") {
            val result = path.cut(path)

            it("should not crash and return an empty path") {
                result shouldEqual Paths.get("")
            }
        }
    }

    given("a path to a valid xml file") {
        on("reading it") {
            val document = SAXReader().read(sampleXmlPath)

            it("should be read correctly") {
                document.selectNodes("//animal").size shouldBe 2
            }
        }
    }

    given("a path to a valid json file") {
        on("reading it") {
            val element = JsonParser().read(sampleJsonPath)

            it("should be read correctly") {
                element.asJsonObject["test"].asInt == 123
            }
        }
    }

    given("a path to a non-existing xml file") {
        val nonExisting = testProject.resolve("non-existing")

        on("reading it") {
            val readCall = { SAXReader().read(nonExisting) }

            it("should throw a proper exception") {
                val expectedMessage = "Could not read xml file: $nonExisting " +
                    "(java.nio.file.NoSuchFileException: $nonExisting)"

                readCall shouldThrow GradleException::class withMessage expectedMessage
            }
        }
    }
})
