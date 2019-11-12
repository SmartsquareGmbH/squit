package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.withExtendedPluginClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeDir
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeFile
import org.amshove.kluent.shouldBeTrue
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
import java.nio.file.Paths
import kotlin.properties.Delegates
import kotlin.streams.toList

/**
 * @author Ruben Gees
 */
object SquitTestTaskSpek : SubjectSpek<Path>({

    subject { Paths.get(this.javaClass.classLoader.getResource("test-project")!!.toURI()) }

    val subjectInvalid3 = File(this.javaClass.classLoader.getResource("invalid-test-project-3")!!.toURI()).toPath()

    val subjectJson = File(this.javaClass.classLoader.getResource("test-project-json")!!.toURI()).toPath()

    val subjectJsonDifferentOrder =
        File(this.javaClass.classLoader.getResource("test-project-json-different-order")!!.toURI()).toPath()

    val subjectIgnoreFailures =
        File(this.javaClass.classLoader.getResource("test-project-ignore-failures")!!.toURI()).toPath()

    val subjectNonStrictXml =
        File(this.javaClass.classLoader.getResource("test-project-xml-not-strict")!!.toURI()).toPath()

    val subjectResponseCode =
        File(this.javaClass.classLoader.getResource("test-project-response-code")!!.toURI()).toPath()

    var server by Delegates.notNull<MockWebServer>()

    val call4Directory = subject
        .resolve("build")
        .resolve("squit")
        .resolve("responses")
        .resolve("raw")
        .resolve("project")
        .resolve("call4")

    val reportsDirectory = subject
        .resolve("build")
        .resolve("squit")
        .resolve("reports")

    val xmlReportPath = reportsDirectory
        .resolve("xml")
        .resolve("index.xml")

    val htmlReportPath = reportsDirectory
        .resolve("html")
        .resolve("index.html")

    val failuresDirectory = reportsDirectory
        .resolve("failures")

    val call1FailuresDirectory = failuresDirectory
        .resolve("project")
        .resolve("call1")

    val call3FailuresDirectory = failuresDirectory
        .resolve("project")
        .resolve("call3")

    val call4FailuresDirectory = failuresDirectory
        .resolve("project")
        .resolve("call4")

    val invalid3Call1Error = subjectInvalid3
        .resolve("build")
        .resolve("squit")
        .resolve("reports")
        .resolve("failures")
        .resolve("project")
        .resolve("call1")
        .resolve("error.txt")

    given("a test project") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should print the amount of executed tests") {
                result.output shouldContain "3 tests ran."
            }

            it("should print the amount of successful, failed and ignored tests") {
                result.output shouldContain "2 successful and 0 failed (1 ignored)."
            }

            it("should generate a xml report") {
                xmlReportPath.toFile().shouldBeFile()
            }

            it("should generate a html report with the correct directory structure") {
                htmlReportPath.toFile().shouldBeFile()

                htmlReportPath.parent.resolve("css").toFile().shouldBeDir()
                htmlReportPath.parent.resolve("detail").toFile().shouldBeDir()
                htmlReportPath.parent.resolve("js").toFile().shouldBeDir()

                htmlReportPath.parent.resolve("detail").resolve("0").resolve("detail.html").toFile().shouldBeFile()
            }

            it("should run, but not report tests which are ignored for report") {
                Files.exists(call4Directory).shouldBeTrue()
                Files.list(failuresDirectory).use { it.toList().shouldBeEmpty() }
            }
        }

        on("running the test task with failing tests") {
            server.enqueue(MockResponse().setBody("<not_cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject", "-Ptags=call1,call2"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should print the amount of successful and failed tests") {
                result.output shouldContain "1 successful and 1 failed."
            }

            it("should generate a \"failures\" directory with all relevant files") {
                Files.exists(call1FailuresDirectory.resolve("actual_response.xml")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("test.conf")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("diff.txt")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("expected_response.xml")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("request.xml")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("test_post.sql")).shouldBeTrue()
                Files.exists(call1FailuresDirectory.resolve("test_pre.sql")).shouldBeTrue()
            }
        }

        on("running the test task with the unignore flag") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject", "-Punignore"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should also report the ignored test") {
                Files.exists(call4FailuresDirectory).shouldBeTrue()
            }

            it("should not report the excluded test") {
                Files.exists(call3FailuresDirectory).shouldBeFalse()
            }
        }

        on("running the test task with the unexclude flag") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subject", "-Punexclude"
            )

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should also report the ignored test") {
                Files.exists(call3FailuresDirectory).shouldBeTrue()
            }

            it("should also report the excluded test") {
                Files.exists(call4FailuresDirectory).shouldBeTrue()
            }
        }
    }

    given("a test project with an error from a previous task") {
        on("running the test task") {
            val arguments = listOf("clean", "squitTest")

            val result = GradleRunner.create()
                .withProjectDir(subjectInvalid3.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should create an error file in the failures directory") {
                Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 4 of document"
            }
        }
    }

    given("a test project with ignoreFailures set to true") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<failure/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectIgnoreFailures"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectIgnoreFailures.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }
    }

    given("a test project with non strict xml diffing") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<abc:cool xmlns:abc='https://example.com'/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectNonStrictXml"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectNonStrictXml.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
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

        on("running the test task") {
            server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectJson"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectJson.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }
    }

    given("a test project with json requests with different order") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the test task") {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                        "abc": false,
                        "cool": true,
                        "olleh": 321,
                        "hello": "123"
                    }
                    """.trimIndent()
                )
            )

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectJsonDifferentOrder"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectJsonDifferentOrder.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }
    }

    given("a test project with an error response code") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the test task with expected response code") {
            server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}").setResponseCode(400))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectResponseCode"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectResponseCode.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        on("running the test task with unexpected response code") {
            server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}").setResponseCode(200))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$subjectResponseCode", "--stacktrace"
            )

            val result = GradleRunner.create()
                .withProjectDir(subjectResponseCode.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }
        }
    }
})
