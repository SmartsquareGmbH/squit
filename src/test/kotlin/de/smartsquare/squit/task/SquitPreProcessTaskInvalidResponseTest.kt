package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldStartWith
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.nio.file.Files

class SquitPreProcessTaskInvalidResponseTest {

    private val invalidProject3 = TestUtils.getResourcePath("invalid-test-project-3")

    private val invalid3Call1Error = invalidProject3.resolve("build/squit/sources/project/call1/error.txt")

    @Test
    fun `normal run`() {
        val arguments = listOf("squitPreProcess")

        val result = gradleRunner(invalidProject3, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
            "org.dom4j.DocumentException: Error on line 4 of document"
    }
}
