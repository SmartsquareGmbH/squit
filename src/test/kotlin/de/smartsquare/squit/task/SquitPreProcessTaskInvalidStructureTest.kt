package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SquitPreProcessTaskInvalidStructureTest {

    private val invalidProject4 = TestUtils.getResourcePath("invalid-test-project-4")

    @Test
    fun `normal run`() {
        val arguments = listOf("squitPreProcess")

        val result = gradleRunner(invalidProject4, arguments).buildAndFail()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FAILED

        result.output shouldContain "Invalid project structure. " +
            "Please add a project directory to the src/squit directory."
    }
}
