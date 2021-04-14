package de.smartsquare.squit.io

import de.smartsquare.squit.TestUtils
import java.io.IOException
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class JsonParserSupportTest {

    private val jsonTestProject = TestUtils.getResourcePath("test-project-json")
    private val sampleJsonPath = jsonTestProject.resolve("src/squit/project/call1/request.json")

    @Test
    fun `reading a path to a valid json file`() {
        val element = JsonParserSupport.read(sampleJsonPath)

        element.asJsonObject["test"].asInt shouldBe 123
    }

    @Test
    fun `reading a path to a non-existing json file`() {
        val nonExisting = jsonTestProject.resolve("non-existing")
        val expectedMessage = "Could not read json file: $nonExisting"
        val readCall = { JsonParserSupport.read(nonExisting) }

        readCall shouldThrow GradleException::class withMessage expectedMessage withCause IOException::class
    }
}
