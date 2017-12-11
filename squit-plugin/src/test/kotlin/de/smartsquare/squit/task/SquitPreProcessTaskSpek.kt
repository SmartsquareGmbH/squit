package de.smartsquare.squit.task

import de.smartsquare.squit.withJaCoCo
import de.smartsquare.squit.withTestClasspath
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * @author Ruben Gees
 */
object SquitPreProcessTaskSpek : SubjectSpek<Path>({

    subject { Paths.get(this.javaClass.classLoader.getResource("test-project").toURI()) }

    val subjectInvalid = Paths.get(this.javaClass.classLoader.getResource("invalid-test-project").toURI())

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

    given("a test project") {
        on("running the pre-process task") {
            val arguments = listOf("clean", "squitPreProcess", "-Pendpoint=https://example.com",
                    "-Ptags=call1,call2,call4")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
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
                println(Files.list(call4PostSqlScript.parent.parent).toList().joinToString())
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
            val arguments = listOf("clean", "squitPreProcess", "-Pendpoint=https://example.com", "-Punignore")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
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

    given("an invalid test project") {
        val arguments = listOf("clean", "squitPreProcess")

        val result = GradleRunner.create()
                .withProjectDir(subjectInvalid.toFile())
                .withArguments(arguments)
                .withTestClasspath()
                .forwardOutput()
                .withJaCoCo()
                .buildAndFail()

        it("should fail the build") {
            result.task(":squitPreProcess")?.outcome shouldBe TaskOutcome.FAILED
        }

        it("should print an appropriate message") {
            result.output shouldContain "Invalid test.conf file on path of test: project/call1 " +
                    "(No configuration setting found for key 'endpoint')"
        }
    }
})
