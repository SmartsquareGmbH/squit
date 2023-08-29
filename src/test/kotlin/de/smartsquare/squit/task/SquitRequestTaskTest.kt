package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.entity.SquitMetaInfo
import de.smartsquare.squit.gradleRunner
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.sql.DriverManager
import java.time.LocalDateTime
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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SquitRequestTaskTest {

    private val project = TestUtils.getResourcePath("test-project")

    private val buildPath = project.resolve("build/squit")
    private val rawResponsesDirectory = buildPath.resolve("responses/raw/project")
    private val call1Response = rawResponsesDirectory.resolve("call1/actual_response.xml")
    private val call1Meta = rawResponsesDirectory.resolve("call1/meta.json")
    private val call1Error = rawResponsesDirectory.resolve("call1/error.txt")
    private val call2Response = rawResponsesDirectory.resolve("call2/actual_response.xml")

    private val jdbc = "jdbc:h2:$project/testDb;IFEXISTS=TRUE"
    private val username = "test"
    private val password = "test"

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(project)
    }

    @Test
    fun `normal run`() {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))

        val arguments = listOf(
            "squitRunRequests",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldNotContain "Could not run database script"

        Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldBeEqualTo "<cool/>"
        Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldBeEqualTo "<nice/>"

        val (date, duration) = SquitMetaInfo.fromJson(
            Files.readAllBytes(call1Meta)
                .toString(Charset.defaultCharset()),
        )

        date shouldBeBefore LocalDateTime.now()
        date shouldBeAfter LocalDateTime.now().minusMinutes(5)
        duration shouldBeInRange 5L..5000L

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

        DriverManager.getConnection(jdbc, username, password).use { connection ->
            val resultSet = connection.createStatement().executeQuery("SELECT * FROM animals")

            resultSet.next()
            resultSet.getString(2) shouldBeEqualTo "brown"
            resultSet.getString(3) shouldBeEqualTo "dog"
            resultSet.next()
            resultSet.getString(2) shouldBeEqualTo "black"
            resultSet.getString(3) shouldBeEqualTo "cat"
        }

        // Files created by pre- and post runners.
        project.resolve("build/pre_run.txt").toFile().shouldExist()
        project.resolve("build/post_run.txt").toFile().shouldExist()
    }

    @Test
    fun `running with a web server returning an error`() {
        server.enqueue(MockResponse().setBody("error").setResponseCode(500))

        val arguments = listOf(
            "squitRunRequests",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1",
            "--info",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "Unsuccessful request for test project${File.separator}call1 (status code: 500)"

        Files.readAllBytes(call1Response).toString(Charsets.UTF_8) shouldBeEqualTo "error"
    }

    @Test
    fun `running with a timeout causing web server`() {
        // Nothing enqueued to cause timeout.

        val arguments = listOf(
            "squitRunRequests",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-Ptags=call1",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(call1Error).toString(Charset.defaultCharset()) shouldStartWith
            "java.net.SocketTimeoutException"
    }

    @Test
    fun `running with a web server answering with a different media type`() {
        server.enqueue(MockResponse().setBody("<cool/>").setHeader("Content-Type", "text/plain"))

        val arguments = listOf(
            "squitRunRequests",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
            "-Ptags=call1",
            "--info",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        result.output shouldContain "Unexpected Media type text/plain for " +
            "test project${File.separator}call1. Expected application/xml"
    }
}
