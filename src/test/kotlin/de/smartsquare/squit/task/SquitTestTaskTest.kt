package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import kotlin.streams.toList
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeDir
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeFile
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitTestTaskTest {

    private val project = TestUtils.getResourcePath("test-project")

    private val call4Directory = project.resolve("build/squit/responses/raw/project/call4")

    private val reportsDirectory = project.resolve("build/squit/reports")
    private val xmlReportPath = reportsDirectory.resolve("xml/index.xml")
    private val htmlReportPath = reportsDirectory.resolve("html/index.html")

    private val failuresDirectory = reportsDirectory.resolve("failures")
    private val call1FailuresDirectory = failuresDirectory.resolve("project/call1")
    private val call3FailuresDirectory = failuresDirectory.resolve("project/call3")
    private val call4FailuresDirectory = failuresDirectory.resolve("project/call4")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(project)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))
        server.enqueue(MockResponse().setBody("<relevant/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS

        result.output shouldContain "3 tests ran."
        result.output shouldContain "2 successful and 0 failed (1 ignored)."

        xmlReportPath.toFile().shouldBeFile()
        htmlReportPath.toFile().shouldBeFile()

        htmlReportPath.parent.resolve("css").toFile().shouldBeDir()
        htmlReportPath.parent.resolve("detail").toFile().shouldBeDir()
        htmlReportPath.parent.resolve("js").toFile().shouldBeDir()
        htmlReportPath.parent.resolve("detail").resolve("0").resolve("detail.html").toFile().shouldBeFile()

        Files.exists(call4Directory).shouldBeTrue()
        Files.list(failuresDirectory).use { it.toList().shouldBeEmpty() }
    }

    @Test
    fun `running with failing tests`() {
        server.enqueue(MockResponse().setBody("<not_cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2",
        )

        val result = gradleRunner(project, arguments).buildAndFail()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED

        result.output shouldContain "1 successful and 1 failed."

        Files.exists(call1FailuresDirectory.resolve("actual_response.xml")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("test.conf")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("diff.txt")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("expected_response.xml")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("request.xml")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("test_post.sql")).shouldBeTrue()
        Files.exists(call1FailuresDirectory.resolve("test_pre.sql")).shouldBeTrue()
    }

    @Test
    fun `running with the unignore flag`() {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))
        server.enqueue(MockResponse().setBody("<relevant/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-Punignore",
        )

        val result = gradleRunner(project, arguments).buildAndFail()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED

        Files.exists(call4FailuresDirectory).shouldBeTrue()
        Files.exists(call3FailuresDirectory).shouldBeFalse()
    }

    @Test
    fun `running with the unexclude flag`() {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))
        server.enqueue(MockResponse().setBody("<relevant/>"))
        server.enqueue(MockResponse().setBody("<relevant/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-Punexclude",
        )

        val result = gradleRunner(project, arguments).buildAndFail()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED

        Files.exists(call3FailuresDirectory).shouldBeTrue()
        Files.exists(call4FailuresDirectory).shouldBeTrue()
    }
}
