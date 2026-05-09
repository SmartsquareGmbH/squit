package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SquitPostProcessTaskJsonProcessorsTest {

    private val project = TestUtils.getResourcePath("test-project-json-processors")

    private val call1Response = project
        .resolve("build/squit/responses/processed/project/call1/actual_response.json")

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
    fun `post-processor adds property to json response`() {
        server.enqueue(MockResponse().setBody("""{ "cool": true }"""))

        val arguments = listOf(
            "squitPostProcess",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldContain """"post": "application/json""""
    }
}
