package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.withJaCoCo
import de.smartsquare.squit.withTestClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeDir
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeFile
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates
import kotlin.streams.toList

/**
 * @author Ruben Gees
 */
object SquitTestTaskSpek : SubjectSpek<Path>({

    subject { Paths.get(this.javaClass.classLoader.getResource("test-project").toURI()) }

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
            .resolve("main.xml")

    val htmlReportPath = reportsDirectory
            .resolve("html")
            .resolve("main.html")

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

            val arguments = listOf("clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                    "-Psquit.rootDir=$subject")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete without errors") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should print the amount of executed tests") {
                result.output shouldContain "2 tests ran."
            }

            it("should generate a xml report") {
                xmlReportPath.toFile().shouldBeFile()
            }

            it("should generate a html report with the correct directory structure") {
                htmlReportPath.toFile().shouldBeFile()

                htmlReportPath.parent.resolve("css").toFile().shouldBeDir()
                htmlReportPath.parent.resolve("detail").toFile().shouldBeDir()
                htmlReportPath.parent.resolve("fonts").toFile().shouldBeDir()
                htmlReportPath.parent.resolve("js").toFile().shouldBeDir()

                htmlReportPath.parent.resolve("detail").resolve("0").resolve("detail.html").toFile().shouldBeFile()
            }

            it("should run, but not report tests which are ignored for report") {
                Files.exists(call4Directory) shouldBe true
                Files.list(failuresDirectory).toList().shouldBeEmpty()
            }
        }

        on("running the test task with failing tests") {
            server.enqueue(MockResponse().setBody("<not_cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf("clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                    "-Psquit.rootDir=$subject", "-Ptags=call1,call2")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should print the amount of successful and failed tests") {
                result.output shouldContain "1 successful and 1 failed."
            }

            it("should generate a \"failures\" directory with all relevant files") {
                Files.exists(call1FailuresDirectory.resolve("actual_response.xml")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("test.conf")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("diff.txt")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("expected_response.xml")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("request.xml")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("test_post.sql")) shouldBe true
                Files.exists(call1FailuresDirectory.resolve("test_pre.sql")) shouldBe true
            }
        }

        on("running the test task with the unignore flag") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf("clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                    "-Psquit.rootDir=$subject", "-Punignore")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should also report the ignored test") {
                Files.exists(call4FailuresDirectory) shouldBe true
            }

            it("should not report the excluded test") {
                Files.exists(call3FailuresDirectory) shouldBe false
            }
        }

        on("running the test task with the unexclude flag") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf("clean", "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                    "-Psquit.rootDir=$subject", "-Punexclude")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .buildAndFail()

            it("should fail the build") {
                result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should also report the ignored test") {
                Files.exists(call3FailuresDirectory) shouldBe true
            }

            it("should also report the excluded test") {
                Files.exists(call4FailuresDirectory) shouldBe true
            }
        }
    }
})
