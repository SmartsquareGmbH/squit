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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.properties.Delegates
import kotlin.streams.toList

object SquitTestTaskSpek : Spek({

    var server by Delegates.notNull<MockWebServer>()

    given("a test project") {
        val project = TestUtils.getResourcePath("test-project")

        val call4Directory = project
            .resolve("build")
            .resolve("squit")
            .resolve("responses")
            .resolve("raw")
            .resolve("project")
            .resolve("call4")

        val reportsDirectory = project
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

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(project)
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$project"
            )

            val result = GradleRunner.create()
                .withProjectDir(project.toFile())
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
                "-Psquit.rootDir=$project", "-Ptags=call1,call2"
            )

            val result = GradleRunner.create()
                .withProjectDir(project.toFile())
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
                "-Psquit.rootDir=$project", "-Punignore"
            )

            val result = GradleRunner.create()
                .withProjectDir(project.toFile())
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
                "-Psquit.rootDir=$project", "-Punexclude"
            )

            val result = GradleRunner.create()
                .withProjectDir(project.toFile())
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
        val invalidProject3 = TestUtils.getResourcePath("invalid-test-project-3")

        val invalid3Call1Error = invalidProject3
            .resolve("build")
            .resolve("squit")
            .resolve("reports")
            .resolve("failures")
            .resolve("project")
            .resolve("call1")
            .resolve("error.txt")

        on("running the test task") {
            val arguments = listOf("clean", "squitTest")

            val result = GradleRunner.create()
                .withProjectDir(invalidProject3.toFile())
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
        val projectIgnoreFailures = TestUtils.getResourcePath("test-project-ignore-failures")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(projectIgnoreFailures)
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<failure/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$projectIgnoreFailures"
            )

            val result = GradleRunner.create()
                .withProjectDir(projectIgnoreFailures.toFile())
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
        val projectWithNonStrictXml = TestUtils.getResourcePath("test-project-xml-not-strict")

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
                "-Psquit.rootDir=$projectWithNonStrictXml"
            )

            val result = GradleRunner.create()
                .withProjectDir(projectWithNonStrictXml.toFile())
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
        val jsonProject = TestUtils.getResourcePath("test-project-json")

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
                "-Psquit.rootDir=$jsonProject"
            )

            val result = GradleRunner.create()
                .withProjectDir(jsonProject.toFile())
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
        val jsonProjectWithDifferentOrder = TestUtils.getResourcePath("test-project-json-different-order")

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
                "-Psquit.rootDir=$jsonProjectWithDifferentOrder"
            )

            val result = GradleRunner.create()
                .withProjectDir(jsonProjectWithDifferentOrder.toFile())
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
        val projectWithResponseCode = TestUtils.getResourcePath("test-project-response-code")

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
                "-Psquit.rootDir=$projectWithResponseCode"
            )

            val result = GradleRunner.create()
                .withProjectDir(projectWithResponseCode.toFile())
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
                "-Psquit.rootDir=$projectWithResponseCode", "--stacktrace"
            )

            val result = GradleRunner.create()
                .withProjectDir(projectWithResponseCode.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }
        }
    }

    given("a test project with different structure") {
        val differentStructureProject = TestUtils.getResourcePath("test-project-different-structure")

        val xmlReportsPath = differentStructureProject
            .resolve("build")
            .resolve("different")
            .resolve("reports")
            .resolve("xml")
            .resolve("index.xml")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the test task") {
            server.enqueue(MockResponse().setBody("<cool/>"))

            val arguments = listOf(
                "clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$differentStructureProject"
            )

            val result = GradleRunner.create()
                .withProjectDir(differentStructureProject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should write reports to correct location") {
                Files.exists(xmlReportsPath).shouldBeTrue()
            }
        }
    }
})
