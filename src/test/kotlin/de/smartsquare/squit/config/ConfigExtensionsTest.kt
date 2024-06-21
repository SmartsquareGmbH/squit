package de.smartsquare.squit.config

import com.typesafe.config.ConfigException.BadValue
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.TestUtils
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.AnyException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ConfigExtensionsTest {

    private val testProject = TestUtils.getResourcePath("test-project")

    @Test
    fun `config object with an endpoint`() {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))

        config.endpoint shouldBeEqualTo "https://example.com".toHttpUrlOrNull()
    }

    @Test
    fun `config object with an invalid endpoint`() {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "invalid"))
        val call = { config.endpoint }

        call shouldThrow AnyException withMessage "Invalid endpoint: invalid"
    }

    @Test
    fun `config object with a mediaType`() {
        val config = ConfigFactory.parseMap(mapOf("mediaType" to "application/xml"))

        config.mediaType shouldBeEqualTo "application/xml".toMediaType()
    }

    @Test
    fun `config object with an invalid mediaType`() {
        val config = ConfigFactory.parseMap(mapOf("mediaType" to "invalid"))
        val call = { config.mediaType }

        call shouldThrow AnyException withMessage "Invalid mediaType: invalid"
    }

    @Test
    fun `config object with a tag`() {
        val config = ConfigFactory.parseMap(mapOf("tags" to listOf("abc")))
        val result = config.mergeTag("def")

        result.getStringList("tags") shouldBeEqualTo listOf("abc", "def")
    }

    @Test
    fun `validating valid config object`() {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))
        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with a valid method`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "method" to "GET"
            )
        )

        config.method shouldBeEqualTo "GET"
    }

    @Test
    fun `config object with no method`() {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))

        config.method shouldBeEqualTo "POST"
    }

    @Test
    fun `config object with a valid testDir`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "testDir" to Paths.get(".").toString()
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid testDir`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "testDir" to Paths.get("does_not_exist").toString()
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing expected file: does_not_exist"
    }

    @Test
    fun `config object with a valid preProcessor`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessors" to listOf("java.lang.String")
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid preProcessor`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessors" to listOf("not.existing")
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing class: not.existing"
    }

    @Test
    fun `config object with a valid preProcessorScript`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessorScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid preProcessorScript`() {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessorScripts" to listOf(notExistingFilePath.toString())
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
    }

    @Test
    fun `config object with a valid postProcessor`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessors" to listOf("java.lang.String")
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid postProcessor`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessors" to listOf("not.existing")
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing class: not.existing"
    }

    @Test
    fun `config object with a valid postProcessorScript`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessorScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid postProcessorScript`() {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessorScripts" to listOf(notExistingFilePath.toString())
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
    }

    @Test
    fun `config object with a valid preRunner`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunners" to listOf("java.lang.String")
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid preRunner`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunners" to listOf("not.existing")
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing class: not.existing"
    }

    @Test
    fun `config object with a valid preRunnerScript`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunnerScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid preRunnerScript`() {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunnerScripts" to listOf(notExistingFilePath.toString())
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
    }

    @Test
    fun `config object with a valid postRunner`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunners" to listOf("java.lang.String")
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid postRunner`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunners" to listOf("not.existing")
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing class: not.existing"
    }

    @Test
    fun `config object with a valid postRunnerScript`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunnerScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid postRunnerScript`() {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunnerScripts" to listOf(notExistingFilePath.toString())
            )
        )

        val call = { config.validate() }

        call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
    }

    @Test
    fun `validating a config object with valid tags`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "tags" to listOf("a", "b")
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `validating a config object with an empty tag`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "tags" to listOf("a", "")
            )
        )

        val call = { config.validate() }

        call shouldThrow AnyException withMessage "tags cannot be empty."
    }

    @Test
    fun `validating a config object with a valid databaseConfiguration`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "databaseConfigurations" to listOf(
                    mapOf(
                        "name" to "test1",
                        "jdbc" to "test2",
                        "username" to "test3",
                        "password" to "test4"
                    )
                )
            )
        )

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `validating a config object with a invalid databaseConfiguration (empty name)`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "databaseConfigurations" to listOf(
                    mapOf(
                        "name" to "",
                        "jdbc" to "test2",
                        "username" to "test3",
                        "password" to "test4"
                    )
                )
            )
        )

        val call = { config.validate() }

        call shouldThrow AnyException withMessage "name of a databaseConfiguration cannot be empty."
    }

    @Test
    fun `validating a config object with a invalid databaseConfiguration (empty jdbc)`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "databaseConfigurations" to listOf(
                    mapOf(
                        "name" to "test1",
                        "jdbc" to "",
                        "username" to "test3",
                        "password" to "test4"
                    )
                )
            )
        )

        val call = { config.validate() }

        call shouldThrow AnyException withMessage "jdbc of a databaseConfiguration cannot be empty."
    }

    @Test
    fun `validating a config object with a invalid databaseConfiguration (empty username)`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "databaseConfigurations" to listOf(
                    mapOf(
                        "name" to "test1",
                        "jdbc" to "test2",
                        "username" to "",
                        "password" to "test4"
                    )
                )
            )
        )

        val call = { config.validate() }

        call shouldThrow AnyException withMessage "username of a databaseConfiguration cannot be empty."
    }

    @Test
    fun `validating a config object with a invalid databaseConfiguration (empty password)`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "databaseConfigurations" to listOf(
                    mapOf(
                        "name" to "test1",
                        "jdbc" to "test2",
                        "username" to "test3",
                        "password" to ""
                    )
                )
            )
        )

        val call = { config.validate() }

        call shouldThrow AnyException withMessage "password of a databaseConfiguration cannot be empty."
    }

    @Test
    fun `config object with valid headers`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "headers" to mapOf(
                    "abc" to "def",
                    "ghi" to "jkl"
                )
            )
        )

        config.headers shouldContain ("abc" to "def")
        config.headers shouldContain ("ghi" to "jkl")

        val call = { config.validate() }

        call shouldNotThrow AnyException
    }

    @Test
    fun `config object with an invalid preTestTask`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preTestTasks" to listOf("[NotExistingTask]"),
            )
        )

        val call = { config.validate() }
        @Suppress("MaxLineLength")
        call shouldThrow BadValue::class withMessage "hardcoded value: Invalid value at 'preTestTasks': " +
            "The enum class SquitPreTestTask has no constant of the name '[NotExistingTask]' " +
            "(should be one of [DATABASE_SCRIPTS, PRE_RUNNERS, PRE_RUNNER_SCRIPTS].)"
    }

    @Test
    fun `config object with an invalid postTestTask`() {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postTestTasks" to listOf("[NotExistingTask]"),
            )
        )

        val call = { config.validate() }
        call shouldThrow BadValue::class withMessage "hardcoded value: Invalid value at 'postTestTasks': " +
            "The enum class SquitPostTestTask has no constant of the name '[NotExistingTask]' " +
            "(should be one of [DATABASE_SCRIPTS, POST_RUNNERS, POST_RUNNER_SCRIPTS].)"
    }
}
