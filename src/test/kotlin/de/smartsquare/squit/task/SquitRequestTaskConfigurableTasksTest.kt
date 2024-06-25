package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeAfter
import org.amshove.kluent.shouldBeBefore
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldExist
import org.amshove.kluent.shouldNotExist
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.time.Instant
import java.util.concurrent.TimeUnit

class SquitRequestTaskConfigurableTasksTest {
    private val project = TestUtils.getResourcePath("test-project-task-config")

    private val jdbc = "jdbc:h2:$project/testDb;IFEXISTS=TRUE"
    private val username = "test"
    private val password = "test"
    private val preRunFile = project.resolve("build/pre_run.txt").toFile()
    private val postRunFile = project.resolve("build/post_run.txt").toFile()

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()

        TestUtils.deleteDatabaseFiles(project)
        preRunFile.delete()
        postRunFile.delete()
    }

    @Test
    fun `should execute pre and post tasks in default order`() {
        server.enqueue(
            MockResponse()
                .setHeadersDelay(10L, TimeUnit.MILLISECONDS)
                .setBody("<cool/>")
        )

        val arguments = listOf(
            "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project", "-Ptags=default"
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS
        val preScriptExecution = Instant.ofEpochMilli(preRunFile.readText().toLong())
        val postScriptExecution = Instant.ofEpochMilli(postRunFile.readText().toLong())
        DriverManager.getConnection(jdbc, username, password).use { connection ->
            val resultSet = connection.createStatement().executeQuery("SELECT * FROM TIMESTAMPS")
            resultSet.next()
            val preDbScriptExecution = resultSet.getTimestamp(2).toInstant()
            resultSet.getString(3) shouldBeEqualTo "TEST_PRE.SQL"
            resultSet.next()
            resultSet.getString(3) shouldBeEqualTo "TEST_POST.SQL"
            val postDbScriptExecution = resultSet.getTimestamp(2).toInstant()
            preDbScriptExecution shouldBeBefore postDbScriptExecution
            preScriptExecution shouldBeBefore postScriptExecution
            preScriptExecution shouldBeBefore preDbScriptExecution
            postScriptExecution shouldBeAfter postDbScriptExecution
        }
    }

    @Test
    fun `should execute scripts in configured order`() {
        server.enqueue(
            MockResponse()
                .setHeadersDelay(10L, TimeUnit.MILLISECONDS)
                .setBody("<cool/>")
        )

        val arguments = listOf(
            "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project", "-Ptags=configured_order"
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        val preScriptExecution = Instant.ofEpochMilli(preRunFile.readText().toLong())
        val postScriptExecution = Instant.ofEpochMilli(postRunFile.readText().toLong())
        DriverManager.getConnection(jdbc, username, password).use { connection ->
            val resultSet = connection.createStatement().executeQuery("SELECT * FROM TIMESTAMPS")
            resultSet.next()
            val preDbScriptExecution = resultSet.getTimestamp(2).toInstant()
            resultSet.getString(3) shouldBeEqualTo "TEST_PRE.SQL"
            resultSet.next()
            resultSet.getString(3) shouldBeEqualTo "TEST_POST.SQL"
            val postDbScriptExecution = resultSet.getTimestamp(2).toInstant()
            preDbScriptExecution shouldBeBefore postDbScriptExecution
            preScriptExecution shouldBeBefore postScriptExecution
            preScriptExecution shouldBeAfter preDbScriptExecution
            postScriptExecution shouldBeBefore postDbScriptExecution
        }
    }

    @Test
    fun `should only execute pre db script and post script`() {
        server.enqueue(
            MockResponse()
                .setHeadersDelay(10L, TimeUnit.MILLISECONDS)
                .setBody("<cool/>")
        )

        val arguments = listOf(
            "squitRunRequests", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project", "-Ptags=only_pre_db_script"
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitRunRequests")?.outcome shouldBe TaskOutcome.SUCCESS

        DriverManager.getConnection(jdbc, username, password).use { connection ->
            val resultSet = connection.createStatement().executeQuery("SELECT * FROM TIMESTAMPS")
            resultSet.next()
            resultSet.getString(3) shouldBeEqualTo "TEST_PRE.SQL"
            resultSet.next().shouldBeFalse()
        }
        preRunFile.shouldNotExist()
        postRunFile.shouldExist()
    }
}
