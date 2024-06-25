package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SquitPreProcessTaskGetTest {

    private val getProject = TestUtils.getResourcePath("test-project-get")

    private val getCall1Directory = getProject.resolve("build/squit/sources/project/call1")

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$getProject",
        )

        val result = gradleRunner(getProject, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(getCall1Directory.resolve("request.xml")).shouldBeFalse()
        Files.exists(getCall1Directory.resolve("description.md")).shouldBeFalse()
    }
}
