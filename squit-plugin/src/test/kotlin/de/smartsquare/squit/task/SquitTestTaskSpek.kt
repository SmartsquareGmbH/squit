package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.withJaCoCo
import de.smartsquare.squit.withTestClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqualTo
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

    val expectedReportPath = Paths.get(this.javaClass.classLoader.getResource("expected-report.xml").toURI())
    val expectedFailureReportPath = Paths.get(this.javaClass.classLoader.getResource("expected-report-failure.xml").toURI())

    val call4Directory = subject
            .resolve("build")
            .resolve("squit")
            .resolve("responses")
            .resolve("raw")
            .resolve("project")
            .resolve("call4")

    val actualReportPath = subject
            .resolve("build")
            .resolve("squit")
            .resolve("reports")
            .resolve("xml")
            .resolve("main.xml")

    val failuresDirectory = subject
            .resolve("build")
            .resolve("squit")
            .resolve("reports")
            .resolve("failures")

    val call1FailuresDirectory = failuresDirectory
            .resolve("project")
            .resolve("call1")

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

            val arguments = listOf("clean", "squitTest", "-Pendpoint=${server.url("/")}")

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

            it("should generate a correct report") {
                val expectedReport = Files.readAllBytes(expectedReportPath).toString(Charsets.UTF_8)
                val actualReport = Files.readAllBytes(actualReportPath).toString(Charsets.UTF_8)

                actualReport shouldEqualTo expectedReport
            }

            it("should run, but not report tests which are ignored for report") {
                Files.exists(call4Directory) shouldBe true
                Files.list(failuresDirectory).toList().shouldBeEmpty()
            }
        }

        on("running the test task with failing tests") {
            server.enqueue(MockResponse().setBody("<not_cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf("clean", "squitTest", "-Pendpoint=${server.url("/")}", "-Ptags=call1,call2")

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

            it("should generate a correct report") {
                val expectedReport = Files.readAllBytes(expectedFailureReportPath).toString(Charsets.UTF_8)
                val actualReport = Files.readAllBytes(actualReportPath).toString(Charsets.UTF_8)

                actualReport shouldEqualTo expectedReport
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

            val arguments = listOf("clean", "squitTest", "-Pendpoint=${server.url("/")}", "-Punignore",
                    "-Ptags=call1,call2,call4")

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
        }

        on("running the test task with the unignoreForReport flag") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))
            server.enqueue(MockResponse().setBody("<relevant/>"))

            val arguments = listOf("clean", "squitTest", "-Pendpoint=${server.url("/")}", "-PunignoreForReport")

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
        }
    }
})
