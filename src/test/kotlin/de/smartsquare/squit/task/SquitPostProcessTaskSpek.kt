package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.withExtendedPluginClasspath
import de.smartsquare.squit.withJaCoCo
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldStartWith
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
object SquitPostProcessTaskSpek : SubjectSpek<Path>({

    subject { File(this.javaClass.classLoader.getResource("test-project")!!.toURI()).toPath() }

    val subjectInvalid3 = File(this.javaClass.classLoader.getResource("invalid-test-project-3")!!.toURI()).toPath()
    val subjectJson = File(this.javaClass.classLoader.getResource("test-project-json")!!.toURI()).toPath()

    var server by Delegates.notNull<MockWebServer>()

    val buildPath = subject
        .resolve("build")
        .resolve("squit")

    val call2Directory = buildPath
        .resolve("responses")
        .resolve("processed")
        .resolve("project")
        .resolve("call2")

    val call2Response = call2Directory
        .resolve("actual_response.xml")

    val call2Error = call2Directory
        .resolve("error.txt")

    val invalid3Call1Error = subjectInvalid3
        .resolve("build")
        .resolve("squit")
        .resolve("responses")
        .resolve("processed")
        .resolve("project")
        .resolve("call1")
        .resolve("error.txt")

    val jsonCall2Response = subjectJson
        .resolve("build")
        .resolve("squit")
        .resolve("responses")
        .resolve("processed")
        .resolve("project")
        .resolve("call1")
        .resolve("actual_response.json")

    given("a test project") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        on("running the post-process task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<test/>"))

            val arguments = listOf(
                "clean", "squitPostProcess", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject", "-Ptags=call1,call2"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should correctly post-process, based on the script") {
                Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldContain "<nice/>"
            }
        }

        on("running the post-process task with a web server returning an invalid answer") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<unclosed_tag>"))

            val arguments = listOf(
                "clean", "squitPostProcess", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject", "-Ptags=call1,call2"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should create an error file") {
                Files.readAllBytes(call2Error).toString(Charsets.UTF_8) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 1 of document"
            }
        }
    }

    given("a test project with an error from a previous task") {
        on("running the post-process task") {
            val arguments = listOf("clean", "squitPostProcess")

            val result = GradleRunner.create()
                .withProjectDir(subjectInvalid3.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should succeed nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should propagate the error file") {
                Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 4 of document"
            }
        }
    }

    given("a test project with json requests") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the post-process task") {
            server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}"))

            val arguments = listOf(
                "clean", "squitPostProcess", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectJson"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectJson.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should correctly post-process") {
                Files.readAllBytes(jsonCall2Response).toString(Charsets.UTF_8) shouldContain "{\n  \"cool\": true\n}"
            }
        }
    }
})
