package de.smartsquare.squit.entity

import de.smartsquare.squit.getResource
import okhttp3.HttpUrl
import okhttp3.MediaType
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Ruben Gees
 */
object SquitPropertiesSpek : SubjectSpek<SquitProperties>({

    subject { SquitProperties() }

    given("a squit properties object") {
        on("filling from not existing properties") {
            it("should do nothing if file does not exist") {
                val shouldThrowFunction = { subject.endpoint }

                subject.fillFromProperties(Paths.get("/not_existing"))

                shouldThrowFunction shouldThrow AssertionError::class
            }
        }

        on("filling from properties") {
            val propertiesFile = getResource("sample.properties")

            subject.fillFromProperties(propertiesFile, mutableMapOf<String, String>())

            it("should have the correct properties set") {
                subject.endpoint shouldEqual HttpUrl.parse("http://example.com")
                subject.mediaType shouldEqual MediaType.parse("application/soap+xml")
                subject.exclude shouldBe true
                subject.ignore shouldBe true
                subject.preProcessors shouldContain "de.smartsquare.squit.PreProcessor"
                subject.postProcessors shouldContain "de.smartsquare.squit.PostProcessor"
                subject.preProcessorScripts.shouldBeEmpty()
                subject.postProcessorScripts.shouldBeEmpty()
                subject.tags shouldContainAll listOf("abc", "def")
                subject.databaseConfigurations shouldContain SquitDatabaseConfiguration("abc",
                        "jdbc:test:@localhost:1234", "abc", "cba")
            }

            it("should order correctly") {
                subject.databaseConfigurations shouldContainAll listOf(
                        SquitDatabaseConfiguration("abc", "jdbc:test:@localhost:1234", "abc", "cba"),
                        SquitDatabaseConfiguration("xyz", "jdbc:xyz:@localhost:1234", "xyz", "zxy")
                )
            }

            it("should automatically add a tag based on directory name") {
                subject.tags shouldContain "${propertiesFile.parent.fileName}"
            }
        }

        on("filling from template properties") {
            val templatePropertiesFile = createTempFile().toPath().also {
                Files.write(it, "endpoint=http://example.com:\$port/api".toByteArray())
            }

            subject.fillFromProperties(templatePropertiesFile, mutableMapOf("port" to "1234"))

            it("should be able to handle templates") {
                subject.endpoint shouldEqual HttpUrl.parse("http://example.com:1234/api")
            }
        }

        on("filling from single properties") {
            val propertiesFile = getResource("sample.properties")

            subject.fillFromSingleProperties(propertiesFile, mutableMapOf<String, String>())

            it("should have the correct properties set") {
                subject.endpoint shouldEqual HttpUrl.parse("http://example.com")
                subject.mediaType shouldEqual MediaType.parse("application/soap+xml")
                subject.exclude shouldBe true
                subject.ignore shouldBe true
                subject.preProcessors shouldContain "de.smartsquare.squit.PreProcessor"
                subject.postProcessors shouldContain "de.smartsquare.squit.PostProcessor"
                subject.preProcessorScripts.shouldBeEmpty()
                subject.postProcessorScripts.shouldBeEmpty()
                subject.tags shouldContainAll listOf("abc", "def")
                subject.databaseConfigurations shouldContain SquitDatabaseConfiguration("abc",
                        "jdbc:test:@localhost:1234", "abc", "cba")
            }
        }

        on("filling from incomplete single properties") {
            val propertiesFile2 = createTempFile().toPath().also { Files.write(it, "exclude=true".toByteArray()) }
            val shouldThrowFunction = { subject.fillFromSingleProperties(propertiesFile2) }

            it("should throw an error") {
                shouldThrowFunction shouldThrow GradleException::class withMessage "Invalid config.properties at " +
                        "path: $propertiesFile2 (endpoint property is missing)"
            }
        }

        on("merging with another properties object") {
            val propertiesFile1 = createTempFile().toPath().also { Files.write(it, "exclude=true".toByteArray()) }
            val propertiesFile2 = createTempFile().toPath().also { Files.write(it, "mediaType=a/b".toByteArray()) }
            val propertiesFile3 = createTempFile().toPath().also { Files.write(it, "exclude=false".toByteArray()) }
            val subject2 = SquitProperties().fillFromProperties(propertiesFile2)
            val subject3 = SquitProperties().fillFromProperties(propertiesFile3)

            subject.fillFromProperties(propertiesFile1)
            subject.mergeWith(subject2)

            it("should have the correct properties set") {
                subject.exclude shouldBe true
                subject.mediaType shouldEqual MediaType.parse("a/b")
            }

            subject.mergeWith(subject3)

            it("should not override existing properties") {
                subject.exclude shouldBe true
            }
        }

        on("writing properties to Java properties") {
            it("should contain the correct values") {
                val propertiesFile = getResource("sample.properties").also { subject.fillFromProperties(it) }
                val javaProperties = subject.writeToProperties()

                javaProperties.getProperty("endpoint") shouldEqual "http://example.com/"
                javaProperties.getProperty("mediaType") shouldEqual "application/soap+xml"
                javaProperties.getProperty("exclude") shouldEqual "true"
                javaProperties.getProperty("ignore") shouldEqual "true"
                javaProperties.getProperty("preProcessors") shouldContain "de.smartsquare.squit.PreProcessor"
                javaProperties.getProperty("postProcessors") shouldContain "de.smartsquare.squit.PostProcessor"
                javaProperties.getProperty("preProcessorScripts") shouldBe null
                javaProperties.getProperty("postProcessorScripts") shouldBe null
                javaProperties.getProperty("tags") shouldEqual "abc,def,${propertiesFile.parent.fileName}"
            }
        }

        on("validating") {
            it("should return an error message if a required property is missing") {
                subject.validateAndGetErrorMessage() shouldEqual "endpoint property is missing"
            }
        }
    }
})
