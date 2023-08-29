package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldStartWith
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitPostProcessTaskTest {

    private val project = TestUtils.getResourcePath("test-project")
    private val buildPath = project.resolve("build/squit")

    private val call2Directory = buildPath.resolve("responses/processed/project/call2")
    private val call2Response = call2Directory.resolve("actual_response.xml")
    private val call2Error = call2Directory.resolve("error.txt")

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
        server.enqueue(MockResponse().setBody("<test/>"))

        val arguments = listOf(
            "squitPostProcess",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        // language=xml
        val expectedResponse = """<nice post="application/xml"/>"""

        Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldContain expectedResponse
    }

    @Test
    fun `running with web server returning an invalid answer`() {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<unclosed_tag>"))

        val arguments = listOf(
            "squitPostProcess",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(call2Error).toString(Charsets.UTF_8) shouldStartWith
            "org.dom4j.DocumentException: Error on line 1 of document"
    }

    @Test
    fun `running with build cache twice`() {
        repeat(2) {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<test/>"))
        }

        val arguments = listOf(
            "squitPostProcess",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2",
            "--build-cache",
        )

        val result = gradleRunner(project, arguments).build()

        TestUtils.deleteDatabaseFiles(project)

        val cacheResult = gradleRunner(project, arguments).build()

        result.task(":squitPostProcess")?.outcome shouldBeIn arrayOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)
        cacheResult.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.FROM_CACHE
    }
}
