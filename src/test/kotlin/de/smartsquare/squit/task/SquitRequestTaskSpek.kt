package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.entity.SquitMetaInfo
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.gradleRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeAfter
import org.amshove.kluent.shouldBeBefore
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldExist
import org.amshove.kluent.shouldNotContain
import org.amshove.kluent.shouldStartWith
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.properties.Delegates

object SquitRequestTaskSpek : Spek({

    var server by Delegates.notNull<MockWebServer>()

    given("a test project") {
        val project = TestUtils.getResourcePath("test-project")

        val buildPath = project
            .resolve("build")
            .resolve("squit")

        val rawResponsesDirectory = buildPath
            .resolve("responses")
            .resolve("raw")
            .resolve("project")

        val call1Response = rawResponsesDirectory
            .resolve("call1")
            .resolve("actual_response.xml")

        val call1Meta = rawResponsesDirectory
            .resolve("call1")
            .resolve("meta.json")

        val call1Error = rawResponsesDirectory
            .resolve("call1")
            .resolve("error.txt")

        val call2Response = rawResponsesDirectory
            .resolve("call2")
            .resolve("actual_response.xml")

        val jdbc = "jdbc:h2:$project/testDb;IFEXISTS=TRUE"
        val username = "test"
        val password = "test"

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(project)
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$project", "-PtagsOr=call1,call2"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should properly receive and save the responses") {
                Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldBeEqualTo "<cool/>"
                Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldBeEqualTo "<nice/>"
            }

            it("should write a valid meta.json file") {
                val (date, duration) = SquitMetaInfo.fromJson(
                    Files.readAllBytes(call1Meta)
                        .toString(Charset.defaultCharset())
                )

                date shouldBeBefore LocalDateTime.now()
                date shouldBeAfter LocalDateTime.now().minusMinutes(5)
                duration shouldBeInRange 5L..5000L
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldBeEqualTo "POST"
                    it.headers["Content-Type"] shouldBeEqualTo "application/xml"
                    it.headers["some"] shouldBeEqualTo "local header"
                }

                server.takeRequest().let {
                    it.method shouldBeEqualTo "POST"
                    it.headers["Content-Type"] shouldBeEqualTo "application/xml"
                    it.headers["some"] shouldBeEqualTo "header"
                }
            }

            it("should properly run pre and post sql scripts") {
                result.output shouldNotContain "Could not run database script"

                DriverManager.getConnection(jdbc, username, password).use { connection ->
                    val resultSet = connection.createStatement().executeQuery("SELECT * FROM ANIMALS")

                    resultSet.next()
                    resultSet.getString(2) shouldBeEqualTo "brown"
                    resultSet.getString(3) shouldBeEqualTo "dog"
                    resultSet.next()
                    resultSet.getString(2) shouldBeEqualTo "black"
                    resultSet.getString(3) shouldBeEqualTo "cat"
                }
            }

            it("should properly run pre and post runner scripts") {
                // Files created by pre- and post runners.
                project.resolve("build/pre_run.txt").toFile().shouldExist()
                project.resolve("build/post_run.txt").toFile().shouldExist()
            }
        }

        on("running the request task with a web server returning an error") {
            server.enqueue(MockResponse().setBody("error").setResponseCode(500))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$project", "-PtagsOr=call1", "--info"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should properly receive and save the error body") {
                Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldBeEqualTo "error"
            }

            it("should print an appropriate info") {
                result.output shouldContain "Unsuccessful request for test project${File.separator}call1 " +
                    "(status code: 500)"
            }
        }

        on("running the request task with a timeout causing web server") {
            // Nothing enqueued to cause timeout.

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$project", "-Ptags=call1"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should generate an error file") {
                Files.readAllBytes(call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "java.net.SocketTimeoutException"
            }
        }

        on("running the request task with a web server answering with a different media type") {
            server.enqueue(MockResponse().setBody("<cool/>").setHeader("Content-Type", "text/plain"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$project", "-Ptags=call1", "--info"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should print an appropriate info") {
                result.output shouldContain "Unexpected Media type text/plain for " +
                    "test project${File.separator}call1. Expected application/xml"
            }
        }
    }

    given("a test project with an invalid sql script") {
        val invalidProject2 = TestUtils.getResourcePath("invalid-test-project-2")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(invalidProject2)
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$invalidProject2"
            )

            val result = gradleRunner(invalidProject2, arguments).build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should print an appropriate warning") {
                result.output shouldContain "Could not run database script test_pre.sql " +
                    "for test project${File.separator}call1"
            }
        }
    }

    given("a test project with an error from a previous task") {
        val invalidProject3 = TestUtils.getResourcePath("invalid-test-project-3")

        val invalid3Call1Error = invalidProject3
            .resolve("build")
            .resolve("squit")
            .resolve("responses")
            .resolve("raw")
            .resolve("project")
            .resolve("call1")
            .resolve("error.txt")

        on("running the request task") {
            val arguments = listOf("squitRunRequests")

            val result = gradleRunner(invalidProject3, arguments).build()

            it("should succeed nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should propagate the error file") {
                Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 4 of document"
            }
        }
    }

    given("a test project with method GET set") {
        val getProject = TestUtils.getResourcePath("test-project-get")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$getProject"
            )

            val result = gradleRunner(getProject, arguments).build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldBeEqualTo "GET"
                    it.headers["Content-Type"] shouldBe null
                }
            }
        }
    }

    given("a test project with method OPTIONS set") {
        val optionsProject = TestUtils.getResourcePath("test-project-options")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$optionsProject"
            )

            val result = gradleRunner(optionsProject, arguments).build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldBeEqualTo "OPTIONS"
                    it.headers["Content-Type"] shouldBeEqualTo "application/xml"
                }

                server.takeRequest().let {
                    it.method shouldBeEqualTo "OPTIONS"
                    it.headers["Content-Type"] shouldBe null
                }
            }
        }
    }

    given("a test project with json requests") {
        val jsonProject = TestUtils.getResourcePath("test-project-json")

        val jsonRawProjectDir = jsonProject
            .resolve("build")
            .resolve("squit")
            .resolve("responses")
            .resolve("raw")
            .resolve("project")

        val jsonCall1Response = jsonRawProjectDir
            .resolve("call1")
            .resolve("actual_response.json")

        val jsonCall1ActualResponseInfo = jsonRawProjectDir
            .resolve("call1")
            .resolve("actual_response_info.json")

        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("{\n  \"cool\": true\n}"))

            val arguments = listOf(
                "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
                "-Psquit.rootDir=$jsonProject"
            )

            val result = gradleRunner(jsonProject, arguments).build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe SUCCESS
            }

            it("should properly receive and save the responses") {
                Files.readAllBytes(jsonCall1Response).toString(Charsets.UTF_8) shouldBeEqualTo "{\n  \"cool\": true\n}"
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.headers["Content-Type"] shouldBeEqualTo "application/json"
                }
            }

            it("should write a valid actual_response_info.json file") {
                val (expectedResponseCode) = SquitResponseInfo.fromJson(
                    Files.readAllBytes(jsonCall1ActualResponseInfo).toString(Charsets.UTF_8)
                )

                expectedResponseCode shouldBeInRange 200..599
            }
        }
    }
})
