package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.io.File
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SquitPreProcessTaskInvalidConfTest {

    private val invalidProject = TestUtils.getResourcePath("invalid-test-project")

    @Test
    fun `normal run`() {
        val arguments = listOf("squitPreProcess")

        val result = gradleRunner(invalidProject, arguments).buildAndFail()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FAILED

        result.output shouldContain "Invalid test.conf or local.conf file on path of test: " +
            "project${File.separator}call1"
    }
}
