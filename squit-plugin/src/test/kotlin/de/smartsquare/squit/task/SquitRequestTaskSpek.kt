package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.entity.SquitMetaInfo
import de.smartsquare.squit.withJaCoCo
import de.smartsquare.squit.withTestClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be in range`
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeAfter
import org.amshove.kluent.shouldBeBefore
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
object SquitRequestTaskSpek : SubjectSpek<Path>({

    subject { File(this.javaClass.classLoader.getResource("test-project").toURI()).toPath() }

    val subjectInvalid = File(this.javaClass.classLoader.getResource("invalid-test-project-2").toURI()).toPath()
    val subjectGet = File(this.javaClass.classLoader.getResource("test-project-get").toURI()).toPath()
    val subjectOptions = File(this.javaClass.classLoader.getResource("test-project-options").toURI()).toPath()

    var server by Delegates.notNull<MockWebServer>()

    val jdbc = "jdbc:h2:$subject/testDb;IFEXISTS=TRUE"
    val username = "test"
    val password = "test"

    val buildPath = subject
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

    val call2Response = rawResponsesDirectory
            .resolve("call2")
            .resolve("actual_response.xml")

    given("a test project") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}", "-Ptags=call1,call2")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should properly receive and save the responses") {
                Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldEqualTo "<cool/>"
                Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldEqualTo "<nice/>"
            }

            it("should write a valid meta.json file") {
                val (date, duration) = SquitMetaInfo.fromJson(Files.readAllBytes(call1Meta)
                        .toString(Charset.defaultCharset()))

                date shouldBeBefore LocalDateTime.now()
                date shouldBeAfter LocalDateTime.now().minusMinutes(5)
                duration `should be in range` 5L..5000L
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldEqualTo "POST"
                    it.headers.get("Content-Type") shouldEqual "application/xml"
                }

                server.takeRequest().let {
                    it.method shouldEqualTo "POST"
                    it.headers.get("Content-Type") shouldEqual "application/xml"
                }
            }

            it("should properly run pre and post sql scripts") {
                result.output shouldNotContain "Could not run database script"

                DriverManager.getConnection(jdbc, username, password).use { connection ->
                    val resultSet = connection.createStatement().executeQuery("SELECT * FROM ANIMALS")

                    resultSet.next()
                    resultSet.getString(2) shouldEqualTo "brown"
                    resultSet.getString(3) shouldEqualTo "dog"
                    resultSet.next()
                    resultSet.getString(2) shouldEqualTo "black"
                    resultSet.getString(3) shouldEqualTo "cat"
                }
            }
        }

        on("running the request task with a web server, returning an error") {
            server.enqueue(MockResponse().setBody("error").setResponseCode(500))

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}", "-Ptags=call1")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should properly receive and save the error body") {
                Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldEqualTo "error"
            }
        }

        on("running the request task with a timeout causing web server") {
            // Nothing enqueued to cause timeout.

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}", "-Ptags=call1")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should print an appropriate warning") {
                result.output shouldContain "Request failed for test project/call1"
            }
        }
    }

    given("a test project with an invalid sql script") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}")

            val result = GradleRunner.create()
                    .withProjectDir(subjectInvalid.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete successfully nonetheless") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should print an appropriate warning") {
                result.output shouldContain "Could not run database script test_pre.sql for test project/call1"
            }
        }
    }

    given("a test project with method GET set") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}")

            val result = GradleRunner.create()
                    .withProjectDir(subjectGet.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldEqualTo "GET"
                    it.headers.get("Content-Type") shouldBe null
                }
            }
        }
    }

    given("a test project with method OPTIONS set") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()
        }

        on("running the request task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<nice/>"))

            val arguments = listOf("clean", "squitRunRequests", "-Pendpoint=${server.url("/")}")

            val result = GradleRunner.create()
                    .withProjectDir(subjectOptions.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete without errors") {
                result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should make correct requests") {
                server.takeRequest().let {
                    it.method shouldEqualTo "OPTIONS"
                    it.headers.get("Content-Type") shouldEqual "application/xml"
                }

                server.takeRequest().let {
                    it.method shouldEqualTo "OPTIONS"
                    it.headers.get("Content-Type") shouldBe null
                }
            }
        }
    }
})
