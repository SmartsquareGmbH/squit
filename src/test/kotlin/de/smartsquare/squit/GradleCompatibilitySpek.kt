package de.smartsquare.squit

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Path
import kotlin.properties.Delegates

object GradleCompatibilitySpek : SubjectSpek<Path>({

    subject { TestUtils.getResourcePath("test-project") }

    var server by Delegates.notNull<MockWebServer>()

    given("a test project") {
        beforeEachTest {
            server = MockWebServer()
        }

        afterEachTest {
            server.shutdown()

            TestUtils.deleteDatabaseFiles(subject)
        }

        mapOf(
            GradleVersion.current().version to null,
            "7.0" to null,
            "6.0.1" to JavaVersion.VERSION_13,
            "5.6.4" to JavaVersion.VERSION_13,
            "5.1.1" to JavaVersion.VERSION_13
        ).forEach { (gradleVersion, javaVersion) ->
            if (javaVersion == null || JavaVersion.current() <= javaVersion) {
                on("running the test task with Gradle version $gradleVersion") {
                    server.enqueue(MockResponse().setBody("<cool/>"))
                    server.enqueue(MockResponse().setBody("<nice/>"))
                    server.enqueue(MockResponse().setBody("<relevant/>"))

                    val arguments = listOf(
                        "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
                        "-Psquit.rootDir=$subject", "--stacktrace"
                    )

                    val result = gradleRunner(subject, arguments, GradleVersion.version(gradleVersion)).build()

                    it("should be able to complete without errors") {
                        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
                    }
                }
            }
        }
    }
})
