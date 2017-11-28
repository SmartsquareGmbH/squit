package de.smartsquare.squit.task

import de.smartsquare.squit.TestUtils
import de.smartsquare.squit.withJaCoCo
import de.smartsquare.squit.withTestClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
object SquitPostProcessTaskSpek : SubjectSpek<Path>({

    subject { File(this.javaClass.classLoader.getResource("test-project").toURI()).toPath() }

    var server by Delegates.notNull<MockWebServer>()

    val buildPath = subject
            .resolve("build")
            .resolve("squit")

    val call2Response = buildPath
            .resolve("responses")
            .resolve("processed")
            .resolve("project")
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

        on("running the post-process task") {
            server.enqueue(MockResponse().setBody("<cool/>"))
            server.enqueue(MockResponse().setBody("<test/>"))

            val arguments = listOf("clean", "squitPostProcess", "-Pendpoint=${server.url("/")}", "-Ptags=call1,call2")

            val result = GradleRunner.create()
                    .withProjectDir(subject.toFile())
                    .withArguments(arguments)
                    .withTestClasspath()
                    .forwardOutput()
                    .withJaCoCo()
                    .build()

            it("should be able to complete without errors") {
                result.task(":squitPostProcess")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            it("should correctly post-process, based on the script") {
                Files.readAllBytes(call2Response).toString(Charsets.UTF_8) shouldContain "<nice/>"
            }
        }
    }
})
