package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitTestTaskDifferentStructureTest {

    private val differentStructureProject = TestUtils.getResourcePath("test-project-different-structure")

    private val xmlReportsPath = differentStructureProject.resolve("build/different/reports/xml/index.xml")

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(differentStructureProject)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<cool/>"))

        val arguments = listOf(
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$differentStructureProject",
        )

        val result = gradleRunner(differentStructureProject, arguments).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(xmlReportsPath).shouldBeTrue()
    }
}
