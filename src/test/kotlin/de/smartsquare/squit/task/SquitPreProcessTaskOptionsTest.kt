package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SquitPreProcessTaskOptionsTest {

    private val optionsProject = TestUtils.getResourcePath("test-project-options")

    private val optionsSourcesPath = optionsProject.resolve("build/squit/sources/project")

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$optionsProject",
        )

        val result = gradleRunner(optionsProject, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(optionsSourcesPath.resolve("call1").resolve("request.xml")).shouldBeTrue()
        Files.exists(optionsSourcesPath.resolve("call2").resolve("request.xml")).shouldBeFalse()
    }
}
