package de.smartsquare.squit.task

import de.smartsquare.squit.withExtendedPluginClasspath
import de.smartsquare.squit.withJaCoCo
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.amshove.kluent.shouldStartWith
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
import java.nio.file.Paths

/**
 * @author Ruben Gees
 */
object SquitPreProcessTaskSpek : SubjectSpek<Path>({

    subject { Paths.get(this.javaClass.classLoader.getResource("test-project").toURI()) }

    val subjectInvalid = Paths.get(this.javaClass.classLoader.getResource("invalid-test-project").toURI())
    val subjectInvalid3 = Paths.get(this.javaClass.classLoader.getResource("invalid-test-project-3").toURI())
    val subjectGet = Paths.get(this.javaClass.classLoader.getResource("test-project-get").toURI())
    val subjectOptions = Paths.get(this.javaClass.classLoader.getResource("test-project-options").toURI())
    val subjectJson = Paths.get(this.javaClass.classLoader.getResource("test-project-json").toURI())

    val buildPath = subject
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

    val call1Request = call1Directory.resolve("request.xml")
    val call1PreSqlScript = call1Directory.resolve("test_pre.sql")
    val call1PostSqlScript = call1Directory.resolve("test_post.sql")

    val call2PreSqlScript = call2Directory.resolve("test_pre.sql")
    val call2PostSqlScript = call2Directory.resolve("test_post.sql")

    val call4PreSqlScript = call4Directory.resolve("test_pre.sql")
    val call4PostSqlScript = call4Directory.resolve("test_post.sql")

    val getCall1Directory = subjectGet
        .resolve("build")
        .resolve("squit")
        .resolve("sources")
        .resolve("project")
        .resolve("call1")

    val optionsSourcesPath = subjectOptions
        .resolve("build")
        .resolve("squit")
        .resolve("sources")
        .resolve("project")

    val invalid3Call1Error = subjectInvalid3
        .resolve("build")
        .resolve("squit")
        .resolve("sources")
        .resolve("project")
        .resolve("call1")
        .resolve("error.txt")

    val jsonCall1Directory = subjectJson
        .resolve("build")
        .resolve("squit")
        .resolve("sources")
        .resolve("project")
        .resolve("call1")

    given("a test project") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$subject", "-Ptags=call1,call2,call4")

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should correctly pre-process requests, based on the script") {
                Files.readAllBytes(call1Request).toString(Charsets.UTF_8) shouldContain "test=\"test\""
            }

            it("should correctly pre-process sql scripts which are applied only once") {
                Files.readAllBytes(call1PreSqlScript).toString(Charsets.UTF_8) shouldContain "INSERT INTO CARS"
                Files.readAllBytes(call1PostSqlScript).toString(Charsets.UTF_8) shouldNotContain "DROP TABLE CARS"

                Files.exists(call2PreSqlScript) shouldBe false
                Files.exists(call2PostSqlScript) shouldBe false

                Files.exists(call4PreSqlScript) shouldBe false
                Files.readAllBytes(call4PostSqlScript).toString(Charsets.UTF_8) shouldContain "DROP TABLE CARS"
            }

            it("should respect the passed tags") {
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call1")) shouldBe true
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call2")) shouldBe true
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call3")) shouldBe false
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")) shouldBe true
            }
        }

        on("running the pre-process task with the unignore flag") {
            val arguments = listOf("clean", "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$subject", "-Punignore")

            val result = GradleRunner.create()
                .withProjectDir(subject.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should also pre-process the ignored test") {
                Files.exists(buildPath.resolve("sources").resolve("project").resolve("call4")) shouldBe true
            }
        }
    }

    given("an invalid test project (invalid test.conf)") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess")

            val result = GradleRunner.create()
                .withProjectDir(subjectInvalid.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .buildAndFail()

            it("should fail the build") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FAILED
            }

            it("should print an appropriate message") {
                result.output shouldContain "Invalid test.conf file on path of test: project${File.separator}call1 " +
                    "(No configuration setting found for key 'endpoint')"
            }
        }
    }

    given("an invalid test project (invalid response.xml)") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess")

            val result = GradleRunner.create()
                .withProjectDir(subjectInvalid3.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should succeed nonetheless") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should generate an error file") {
                Files.readAllBytes(invalid3Call1Error).toString(Charset.defaultCharset()) shouldStartWith
                    "org.dom4j.DocumentException: Error on line 4 of document"
            }
        }
    }

    given("a test project containing a test with method GET set") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$subject")

            val result = GradleRunner.create()
                .withProjectDir(subjectGet.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should not require or create a request file") {
                Files.exists(getCall1Directory.resolve("request.xml")) shouldBe false
            }
        }
    }

    given("a test project containing tests with method OPTIONS set") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$subject")

            val result = GradleRunner.create()
                .withProjectDir(subjectOptions.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should copy the request file for a test with one") {
                Files.exists(optionsSourcesPath.resolve("call1").resolve("request.xml")) shouldBe true
            }

            it("should not require or create a request file for a test with none") {
                Files.exists(optionsSourcesPath.resolve("call2").resolve("request.xml")) shouldBe false
            }
        }
    }

    given("a test project with json requests") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess", "-Psquit.endpointPlaceholder=https://example.com",
                "-Psquit.rootDir=$subjectJson")

            val result = GradleRunner.create()
                .withProjectDir(subjectJson.toFile())
                .withExtendedPluginClasspath()
                .withArguments(arguments)
                .forwardOutput()
                .withJaCoCo()
                .build()

            it("should be able to complete without error") {
                result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should copy the request file") {
                Files.exists(jsonCall1Directory.resolve("request.json")) shouldBe true
            }
        }
    }
})
