package de.smartsquare.squit

import java.util.stream.Stream
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class GradleCompatibilityTest {

    private companion object {

        @JvmStatic
        private fun provideVersions(): Stream<Arguments> {
            val result = mutableListOf(
                Arguments.of(GradleVersion.current()),
                Arguments.of(GradleVersion.version("7.0"))
            )

            // These older Gradle Versions do not work Java 14+.
            if (JavaVersion.current() <= JavaVersion.VERSION_13) {
                result += listOf(
                    Arguments.of(GradleVersion.version("6.0.1"), JavaVersion.VERSION_13),
                    Arguments.of(GradleVersion.version("5.6.4"), JavaVersion.VERSION_13),
                    Arguments.of(GradleVersion.version("5.1.1"), JavaVersion.VERSION_13),
                )
            }

            return result.stream()
        }
    }

    private val project = TestUtils.getResourcePath("test-project")

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

    @ParameterizedTest
    @MethodSource("provideVersions")
    fun compatibility(gradleVersion: GradleVersion) {
        server.enqueue(MockResponse().setBody("<cool/>"))
        server.enqueue(MockResponse().setBody("<nice/>"))
        server.enqueue(MockResponse().setBody("<relevant/>"))

        val arguments = listOf(
            "squitTest", "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project", "--stacktrace"
        )

        val result = gradleRunner(project, arguments, gradleVersion).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
