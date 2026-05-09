package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SquitPreProcessTaskJsonProcessorsTest {

    private val project = TestUtils.getResourcePath("test-project-json-processors")

    private val call1Directory = project.resolve("build/squit/sources/project/call1")
    private val call1Request = call1Directory.resolve("request.json")

    @Test
    fun `pre-processor adds property to json request`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain """"pre": true"""
    }
}
