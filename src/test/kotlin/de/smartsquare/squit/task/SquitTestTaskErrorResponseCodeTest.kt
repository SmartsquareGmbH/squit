package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitTestTaskErrorResponseCodeTest {

    private val projectWithResponseCode = TestUtils.getResourcePath("test-project-response-code")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(projectWithResponseCode)
    }

    @Test
    fun `running with expected response code`() {
        server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}").setResponseCode(400))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$projectWithResponseCode",
        )

        val result = gradleRunner(projectWithResponseCode, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `running with unexpected response code`() {
        server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}").setResponseCode(200))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$projectWithResponseCode",
        )

        val result = gradleRunner(projectWithResponseCode, arguments).buildAndFail()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.FAILED
    }
}
