package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import java.nio.file.Files
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SquitPreProcessTaskTest {

    private val project = TestUtils.getResourcePath("test-project")

    private val buildPath = project.resolve("build/squit")
    private val call1Directory = buildPath.resolve("sources/project/call1")
    private val call2Directory = buildPath.resolve("sources/project/call2")
    private val call4Directory = buildPath.resolve("sources/project/call4")

    private val call1Config = call1Directory.resolve("test.conf")
    private val call1Request = call1Directory.resolve("request.xml")
    private val call1PreSqlScript = call1Directory.resolve("test_pre.sql")
    private val call1PostSqlScript = call1Directory.resolve("test_post.sql")
    private val call1Description = call1Directory.resolve("description.md")

    private val call2Config = call2Directory.resolve("test.conf")
    private val call2PreSqlScript = call2Directory.resolve("test_pre.sql")
    private val call2PostSqlScript = call2Directory.resolve("test_post.sql")

    private val call4PreSqlScript = call4Directory.resolve("test_pre.sql")
    private val call4PostSqlScript = call4Directory.resolve("test_post.sql")

    @Test
    fun `normal run`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-PtagsOr=call1,call2,call4",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain "test=\"test\""
        Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain "pre=\"true\""
        Files.readAllBytes(call1PreSqlScript).toString(Charsets.UTF_8) shouldContain "INSERT INTO CARS"
        Files.readAllBytes(call1PostSqlScript).toString(Charsets.UTF_8) shouldNotContain "DROP TABLE CARS"

        Files.exists(call2PreSqlScript).shouldBeFalse()
        Files.exists(call2PostSqlScript).shouldBeFalse()

        Files.exists(call4PreSqlScript).shouldBeFalse()
        Files.readAllBytes(call4PostSqlScript).toString(Charsets.UTF_8) shouldContain "DROP TABLE CARS"

        val expected = """
            # Root description

            This is the root description.

            # Individual description

            This is an individual description.

        """.trimIndent()

        val actualString = Files.readAllBytes(call1Description).toString(Charsets.UTF_8)
        val actual = actualString.lines().joinToString("\n") { it.trim() }

        actual shouldBeEqualTo expected

        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call1")).shouldBeTrue()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call2")).shouldBeTrue()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call3")).shouldBeFalse()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeTrue()

        Files.readAllBytes(call1Config).toString(Charsets.UTF_8) shouldContain "some=\"local header\""
        Files.readAllBytes(call2Config).toString(Charsets.UTF_8) shouldContain "some=header"
    }

    @Test
    fun `running with tags`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-PtagsAnd=project,unique",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call1")).shouldBeTrue()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call2")).shouldBeFalse()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call3")).shouldBeFalse()
        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeFalse()
    }

    @Test
    fun `running with the unignore flag`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-Psquit.titlePlaceholder=newTitle",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS

        Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeTrue()

        Files.readAllBytes(call2Config).toString(Charsets.UTF_8) shouldContain "title=newTitle"
    }

    @Test
    fun `running with overriding config`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-Punignore",
        )

        val result = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `running with build cache twice`() {
        val arguments = listOf(
            "squitPreProcess",
            "-Psquit.endpointPlaceholder=https://example.com",
            "-Psquit.rootDir=$project",
            "-Punignore",
            "--build-cache",
        )

        val result = gradleRunner(project, arguments).build()
        val cacheResult = gradleRunner(project, arguments).build()

        result.task(":squitPreProcess")?.outcome shouldBeIn arrayOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)
        cacheResult.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FROM_CACHE
    }
}
