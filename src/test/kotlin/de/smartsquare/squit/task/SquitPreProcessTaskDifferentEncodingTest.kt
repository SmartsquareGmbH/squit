package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitPreProcessTaskDifferentEncodingTest {

    private val project = TestUtils.getResourcePath("test-project")

    private val call2Directory = project.resolve("src/squit/project/call2")
    private val testFile = call2Directory.resolve("test_pre.sql")

    @BeforeEach
    fun setUp() {
        Files.write(testFile, listOf("ß"), Charsets.ISO_8859_1, CREATE, TRUNCATE_EXISTING)
    }

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(testFile)
    }

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2,call4",
        )

        val result = gradleRunner(project, arguments).buildAndFail()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain "Squit expects UTF-8 encoded files only"
    }
}
