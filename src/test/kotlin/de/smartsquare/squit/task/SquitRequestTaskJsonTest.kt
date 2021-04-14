package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitRequestTaskJsonTest {

    private val jsonProject = TestUtils.getResourcePath("test-project-json")

    private val jsonRawProjectDir = jsonProject.resolve("build/squit/responses/raw/project")
    private val jsonCall1Response = jsonRawProjectDir.resolve("call1/actual_response.json")
    private val jsonCall1ActualResponseInfo = jsonRawProjectDir.resolve("call1/actual_response_info.json")

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
            "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$jsonProject"
        )

        val result = gradleRunner(jsonProject, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(jsonCall1Response).toString(Charsets.UTF_8) shouldBeEqualTo "{\n  \"cool\": true\n}"

        server.takeRequest().let {
            it.headers["Content-Type"] shouldBeEqualTo "application/json"
        }

        val (expectedResponseCode) = SquitResponseInfo.fromJson(
            Files.readAllBytes(jsonCall1ActualResponseInfo).toString(Charsets.UTF_8)
        )

        expectedResponseCode shouldBeInRange 200..599
    }
}
