package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SquitPreProcessTaskJsonTest {

    private val jsonProject = TestUtils.getResourcePath("test-project-json")

    private val jsonCall1Directory = jsonProject.resolve("build/squit/sources/project/call1")

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$jsonProject"
        )

        val result = gradleRunner(jsonProject, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(jsonCall1Directory.resolve("request.json")).shouldBeTrue()
    }
}
