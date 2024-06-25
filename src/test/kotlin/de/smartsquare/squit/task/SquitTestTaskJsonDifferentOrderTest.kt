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

class SquitTestTaskJsonDifferentOrderTest {

    private val jsonProjectWithDifferentOrder = TestUtils.getResourcePath("test-project-json-different-order")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(jsonProjectWithDifferentOrder)
    }

    @Test
    fun `normal run`() {
        server.enqueue(
            // language=json
            MockResponse().setBody(
                """
                    {
                      "abc": false,
                      "cool": true,
                      "olleh": 321,
                      "hello": "123"
                    }
                """.trimIndent(),
            ),
        )

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$jsonProjectWithDifferentOrder",
        )

        val result = gradleRunner(jsonProjectWithDifferentOrder, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
