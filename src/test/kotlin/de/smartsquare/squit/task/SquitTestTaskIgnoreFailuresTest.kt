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

class SquitTestTaskIgnoreFailuresTest {

    private val projectIgnoreFailures = TestUtils.getResourcePath("test-project-ignore-failures")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(projectIgnoreFailures)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<failure/>"))

        val arguments = listOf(
            "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$projectIgnoreFailures"
        )

        val result = gradleRunner(projectIgnoreFailures, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
