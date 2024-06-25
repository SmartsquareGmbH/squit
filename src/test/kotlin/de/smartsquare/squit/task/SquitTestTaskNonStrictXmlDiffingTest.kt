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

class SquitTestTaskNonStrictXmlDiffingTest {

    private val projectWithNonStrictXml = TestUtils.getResourcePath("test-project-xml-not-strict")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(projectWithNonStrictXml)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<abc:cool xmlns:abc='https://example.com'/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$projectWithNonStrictXml",
        )

        val result = gradleRunner(projectWithNonStrictXml, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
