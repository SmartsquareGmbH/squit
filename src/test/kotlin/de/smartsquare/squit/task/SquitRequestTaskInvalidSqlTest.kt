package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.io.File
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitRequestTaskInvalidSqlTest {

    private val invalidProject2 = TestUtils.getResourcePath("invalid-test-project-2")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(invalidProject2)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<cool/>"))

        val arguments = listOf(
            "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$invalidProject2"
        )

        val result = gradleRunner(invalidProject2, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        result.output shouldContain "Could not run database script test_pre.sql " +
            "for test project${File.separator}call1"
    }
}
