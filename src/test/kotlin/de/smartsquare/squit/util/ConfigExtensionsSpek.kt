package de.smartsquare.squit.util

import com.typesafe.config.ConfigFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.amshove.kluent.AnyException
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.gradle.api.GradleException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File

/**
 * @author Ruben Gees
 */
object ConfigExtensionsSpek : Spek({

    val testProject = File(this.javaClass.classLoader.getResource("test-project")!!.toURI()).toPath()

    given("a config object with an endpoint") {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))

        on("getting the endpoint") {
            val endpoint = config.endpoint

            it("should be parsed correctly") {
                endpoint shouldEqual "https://example.com".toHttpUrlOrNull()
            }
        }
    }

    given("a config object with an invalid endpoint") {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "invalid"))

        on("getting the endpoint") {
            val call = { config.endpoint }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage "Invalid endpoint: invalid"
            }
        }
    }

    given("a config object with a mediaType") {
        val config = ConfigFactory.parseMap(mapOf("mediaType" to "application/xml"))

        on("getting the mediaType") {
            val mediaType = config.mediaType

            it("should be parsed correctly") {
                mediaType shouldEqual "application/xml".toMediaTypeOrNull()
            }
        }
    }

    given("a config object with an invalid mediaType") {
        val config = ConfigFactory.parseMap(mapOf("mediaType" to "invalid"))

        on("getting the mediaType") {
            val call = { config.mediaType }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage "Invalid mediaType: invalid"
            }
        }
    }

    given("a config object with a tag") {
        val config = ConfigFactory.parseMap(mapOf("tags" to listOf("abc")))

        on("merging with another tag") {
            val result = config.mergeTag("def")

            it("should be merged correctly") {
                result.getStringList("tags") shouldEqual listOf("abc", "def")
            }
        }
    }

    given("a valid config object") {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with a valid method") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "method" to "GET"
            )
        )

        on("getting the method") {
            val method = config.method

            it("should return the given method") {
                method shouldEqual "GET"
            }
        }
    }

    given("a config object with no method") {
        val config = ConfigFactory.parseMap(mapOf("endpoint" to "https://example.com"))

        on("getting the method") {
            val method = config.method

            it("should fallback to the default POST method") {
                method shouldEqual "POST"
            }
        }
    }

    given("a config object with an invalid preProcessor") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessors" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid preProcessor") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessors" to listOf("java.lang.String")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid preProcessor") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessors" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid preProcessorScript") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessorScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid preProcessorScript") {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preProcessorScripts" to listOf(notExistingFilePath.toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
            }
        }
    }

    given("a config object with a valid postProcessor") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessors" to listOf("java.lang.String")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid postProcessor") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessors" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid postProcessorScript") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessorScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid postProcessorScript") {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postProcessorScripts" to listOf(notExistingFilePath.toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
            }
        }
    }

    given("a config object with an invalid preRunner") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunners" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid preRunner") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunners" to listOf("java.lang.String")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid preRunner") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunners" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid preRunnerScript") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunnerScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid preRunnerScript") {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "preRunnerScripts" to listOf(notExistingFilePath.toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
            }
        }
    }

    given("a config object with a valid postRunner") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunners" to listOf("java.lang.String")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid postRunner") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunners" to listOf("not.existing")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow ClassNotFoundException::class withMessage "not.existing"
            }
        }
    }

    given("a config object with a valid postRunnerScript") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunnerScripts" to listOf(testProject.resolve("build.gradle").toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an invalid postRunnerScript") {
        val notExistingFilePath = testProject.resolve("not/existing")
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "postRunnerScripts" to listOf(notExistingFilePath.toString())
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow GradleException::class withMessage "Missing expected file: $notExistingFilePath"
            }
        }
    }

    given("a config object with valid tags") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "tags" to listOf("a", "b")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with an empty tag") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "tags" to listOf("a", "")
            )
        )

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage "tags cannot be empty."
            }
        }
    }

    given("a config object with a valid databaseConfiguration") {
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

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }

    given("a config object with a invalid databaseConfiguration (empty name)") {
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

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage
                    "name of a databaseConfiguration cannot be empty."
            }
        }
    }

    given("a config object with a invalid databaseConfiguration (empty jdbc)") {
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

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage
                    "jdbc of a databaseConfiguration cannot be empty."
            }
        }
    }

    given("a config object with a invalid databaseConfiguration (empty username)") {
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

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage
                    "username of a databaseConfiguration cannot be empty."
            }
        }
    }

    given("a config object with a invalid databaseConfiguration (empty password)") {
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

        on("validating") {
            val call = { config.validate() }

            it("should throw a proper exception") {
                call shouldThrow IllegalStateException::class withMessage
                    "password of a databaseConfiguration cannot be empty."
            }
        }
    }

    given(" a config object with valid headers") {
        val config = ConfigFactory.parseMap(
            mapOf(
                "endpoint" to "https://example.com",
                "headers" to mapOf(
                    "abc" to "def",
                    "ghi" to "jkl"
                )
            )
        )

        on("getting the headers") {
            val headers = config.headers

            it("should be parsed correctly") {
                headers shouldContain ("abc" to "def")
                headers shouldContain ("ghi" to "jkl")
            }
        }

        on("validating") {
            val call = { config.validate() }

            it("should not throw") {
                call shouldNotThrow AnyException
            }
        }
    }
})
