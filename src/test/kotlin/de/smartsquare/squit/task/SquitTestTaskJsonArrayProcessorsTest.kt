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

class SquitTestTaskJsonArrayProcessorsTest {

    private val project = TestUtils.getResourcePath("test-project-json-array-processors")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `normal run with json array processors`() {
        server.enqueue(MockResponse().setBody("[true]"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
