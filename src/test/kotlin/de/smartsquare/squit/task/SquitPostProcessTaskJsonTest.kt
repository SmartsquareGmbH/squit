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

class SquitPostProcessTaskJsonTest {

    private val jsonProject = TestUtils.getResourcePath("test-project-json")

    private val jsonCall2Response = jsonProject
        .resolve("build/squit/responses/processed/project/call1/actual_response.json")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(jsonProject)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}"))

        val arguments = listOf(
            "squitPostProcess",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$jsonProject",
        )

        val result = gradleRunner(jsonProject, arguments).build()

        result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(jsonCall2Response).toString(Charsets.UTF_8) shouldContain "{\n  \"cool\": true\n}"
    }
}
