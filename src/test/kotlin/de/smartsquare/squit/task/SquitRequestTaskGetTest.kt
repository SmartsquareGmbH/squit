package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitRequestTaskGetTest {

    private val getProject = TestUtils.getResourcePath("test-project-get")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(getProject)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<cool/>"))

        val arguments = listOf(
            "squitRunRequests",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$getProject",
        )

        val result = gradleRunner(getProject, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        server.takeRequest().let {
            it.method shouldBeEqualTo "GET"
            it.headers["Content-Type"] shouldBe null
        }
    }
}
