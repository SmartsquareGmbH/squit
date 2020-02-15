package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.gradleRunner
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.amshove.kluent.shouldStartWith
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files

object SquitPreProcessTaskSpek : Spek({

    given("a test project") {
        val project = TestUtils.getResourcePath("test-project")

        val buildPath = project
            .resolve("build")
            .resolve("squit")

        val call1Directory = buildPath
            .resolve("sources")
            .resolve("project")
            .resolve("call1")

        val call2Directory = buildPath
            .resolve("sources")
            .resolve("project")
            .resolve("call2")

        val call4Directory = buildPath
            .resolve("sources")
            .resolve("project")
            .resolve("call4")

        val call1Config = call1Directory.resolve("test.conf")
        val call1Request = call1Directory.resolve("request.xml")
        val call1PreSqlScript = call1Directory.resolve("test_pre.sql")
        val call1PostSqlScript = call1Directory.resolve("test_post.sql")
        val call1Description = call1Directory.resolve("description.md")

        val call2Config = call2Directory.resolve("test.conf")
        val call2PreSqlScript = call2Directory.resolve("test_pre.sql")
        val call2PostSqlScript = call2Directory.resolve("test_post.sql")

        val call4PreSqlScript = call4Directory.resolve("test_pre.sql")
        val call4PostSqlScript = call4Directory.resolve("test_post.sql")

        on("running the pre-process task") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$project", "-PtagsOr=call1,call2,call4"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should correctly pre-process requests, based on the script") {
                Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain "test=\"test\""
            }

            it("should correctly pre-process requests, based on the class") {
                Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain "pre=\"true\""
            }

            it("should correctly pre-process sql scripts which are applied only once") {
                Files.readAllBytes(call1PreSqlScript).toString(Charsets.UTF_8) shouldContain "INSERT INTO CARS"
                Files.readAllBytes(call1PostSqlScript).toString(Charsets.UTF_8) shouldNotContain "DROP TABLE CARS"

                Files.exists(call2PreSqlScript).shouldBeFalse()
                Files.exists(call2PostSqlScript).shouldBeFalse()

                Files.exists(call4PreSqlScript).shouldBeFalse()
                Files.readAllBytes(call4PostSqlScript).toString(Charsets.UTF_8) shouldContain "DROP TABLE CARS"
            }

            it("should correctly merge description files") {
                val expected = """
                    # Root description

                    This is the root description.

                    # Individual description

                    This is an individual description.

                """.trimIndent()

                val actualString = Files.readAllBytes(call1Description).toString(Charsets.UTF_8)
                val actual = actualString.lines().joinToString("\n") { it.trim() }

                actual shouldBeEqualTo expected
            }

            it("should respect the passed tags") {
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call1")).shouldBeTrue()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call2")).shouldBeTrue()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call3")).shouldBeFalse()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeTrue()
            }

            it("should merge configs in the correct order") {
                Files.readAllBytes(call1Config).toString(Charsets.UTF_8) shouldContain "some=\"local header\""
                Files.readAllBytes(call2Config).toString(Charsets.UTF_8) shouldContain "some=header"
            }
        }

        on("running the pre-process task with and tags") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$project", "-PtagsAnd=project,unique"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should respect the passed tags") {
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call1")).shouldBeTrue()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call2")).shouldBeFalse()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call3")).shouldBeFalse()
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeFalse()
            }
        }

        on("running the pre-process task with the unignore flag") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$project", "-Psquit.titlePlaceholder=newTitle"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should also pre-process the ignored test") {
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")).shouldBeTrue()
            }

            it("should merge configs in the correct order") {
                Files.readAllBytes(call2Config).toString(Charsets.UTF_8) shouldContain "title=newTitle"
            }
        }

        on("running the pre-process task with overriding config") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$project", "-Punignore"
            )

            val result = gradleRunner(project, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }
        }

        on("running the pre-process task with build cache twice") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$project", "-Punignore", "--build-cache"
            )

            val result = gradleRunner(project, arguments).build()
            val cacheResult = gradleRunner(project, arguments).build()

            it("should cache") {
                result.task(":squitPreProcess")?.outcome shouldBeIn arrayOf(SUCCESS, FROM_CACHE)
                cacheResult.task(":squitPreProcess")?.outcome shouldBe FROM_CACHE
            }
        }
    }

    given("an invalid test project (invalid test.conf)") {
        val invalidProject = TestUtils.getResourcePath("invalid-test-project")

        on("running the pre-process task") {
            val arguments = listOf("squitPreProcess")

            val result = gradleRunner(invalidProject, arguments).buildAndFail()

            it("should fail the build") {
                result.task(":squitPreProcess")?.outcome shouldBe FAILED
            }

            it("should print an appropriate message") {
                result.output shouldContain "Invalid test.conf or local.conf file on path of test: " +
                    "project${File.separator}call1"
            }
        }
    }

    given("an invalid test project (invalid response.xml)") {
        val invalidProject3 = TestUtils.getResourcePath("invalid-test-project-3")

        val invalid3Call1Error = invalidProject3
            .resolve("build")
            .resolve("squit")
            .resolve("sources")
            .resolve("project")
            .resolve("call1")
            .resolve("error.txt")

        on("running the pre-process task") {
            val arguments = listOf("squitPreProcess")

            val result = gradleRunner(invalidProject3, arguments).build()

            it("should succeed nonetheless") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should generate an error file") {
                Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 4 of document"
            }
        }
    }

    given("an invalid test project (invalid structure)") {
        val invalidProject4 = TestUtils.getResourcePath("invalid-test-project-4")

        on("running the pre-process task") {
            val arguments = listOf("squitPreProcess")

            val result = gradleRunner(invalidProject4, arguments).buildAndFail()

            it("should fail the build") {
                result.task(":squitPreProcess")?.outcome shouldBe FAILED
            }

            it("should print an appropriate message") {
                result.output shouldContain "Invalid project structure. " +
                    "Please add a project directory to the src/squit directory."
            }
        }
    }

    given("a test project containing a test with method GET set") {
        val getProject = TestUtils.getResourcePath("test-project-get")

        val getCall1Directory = getProject
            .resolve("build")
            .resolve("squit")
            .resolve("sources")
            .resolve("project")
            .resolve("call1")

        on("running the pre-process task") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com", "-Psquit.rootDir=$getProject"
            )

            val result = gradleRunner(getProject, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should not require or create a request file") {
                Files.exists(getCall1Directory.resolve("request.xml")).shouldBeFalse()
            }

            it("should not generate a description file, because the project does not contain one") {
                Files.exists(getCall1Directory.resolve("description.md")).shouldBeFalse()
            }
        }
    }

    given("a test project containing tests with method OPTIONS set") {
        val optionsProject = TestUtils.getResourcePath("test-project-options")

        val optionsSourcesPath = optionsProject
            .resolve("build")
            .resolve("squit")
            .resolve("sources")
            .resolve("project")

        on("running the pre-process task") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com", "-Psquit.rootDir=$optionsProject"
            )

            val result = gradleRunner(optionsProject, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should copy the request file for a test with one") {
                Files.exists(optionsSourcesPath.resolve("call1").resolve("request.xml")).shouldBeTrue()
            }

            it("should not require or create a request file for a test with none") {
                Files.exists(optionsSourcesPath.resolve("call2").resolve("request.xml")).shouldBeFalse()
            }
        }
    }

    given("a test project with json requests") {
        val jsonProject = TestUtils.getResourcePath("test-project-json")

        val jsonCall1Directory = jsonProject
            .resolve("build")
            .resolve("squit")
            .resolve("sources")
            .resolve("project")
            .resolve("call1")

        on("running the pre-process task") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$jsonProject"
            )

            val result = gradleRunner(jsonProject, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }

            it("should copy the request file") {
                Files.exists(jsonCall1Directory.resolve("request.json")).shouldBeTrue()
            }
        }
    }

    given("a test project with multiple placeholders") {
        val projectWithPlaceholders = TestUtils.getResourcePath("test-project-placeholders")

        on("running the pre-process task with only placeholders of tests to run set") {
            val arguments = listOf(
                "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com", "-Ptags=call2",
                "-Psquit.rootDir=$projectWithPlaceholders", "-Psquit.placeholder2=test"
            )

            val result = gradleRunner(projectWithPlaceholders, arguments).build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe SUCCESS
            }
        }
    }
})
