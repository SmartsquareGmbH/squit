package de.smartsquare.squit.io

import de.smartsquare.squit.TestUtils
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import java.io.IOException

class SAXReaderSupportTest {

    private val testProject = TestUtils.getResourcePath("test-project")
    private val sampleXmlPath = testProject.resolve("src/squit/project/call1/request.xml")

    @Test
    fun `reading a path to a valid xml file`() {
        val document = SAXReaderSupport.read(sampleXmlPath)

        document.selectNodes("//animal").size shouldBe 2
    }

    @Test
    fun `reading a path to a non-existing xml file`() {
        val nonExisting = testProject.resolve("non-existing")
        val expectedMessage = "Could not read xml file: $nonExisting"
        val readCall = { SAXReaderSupport.read(nonExisting) }

        readCall shouldThrow GradleException::class withMessage expectedMessage withCause IOException::class
    }
}
