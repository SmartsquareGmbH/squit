package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SquitPreProcessTaskPlaceholderTest {

    private val projectWithPlaceholders = TestUtils.getResourcePath("test-project-placeholders")

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Ptags=call2",
            "-Psquit.rootDir=$projectWithPlaceholders",
            "-Psquit.placeholder2=test",
        )

        val result = gradleRunner(projectWithPlaceholders, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
