package de.smartsquare.squit

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledForJreRange
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class GradleCompatibilityTest {

    companion object {

        @JvmStatic
        fun provideVersions(): Stream<Arguments> {
            val result = mutableListOf(
                Arguments.of(GradleVersion.current())
            )

            // These older Gradle Versions do not work on Java 17+.
            if (JavaVersion.current() <= JavaVersion.VERSION_16) {
                result += listOf(
                    Arguments.of(GradleVersion.version("7.0"))
                )
            }

            // These older Gradle Versions do not work on Java 16+.
            if (JavaVersion.current() <= JavaVersion.VERSION_15) {
                result += listOf(
                    Arguments.of(GradleVersion.version("6.8"))
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

    @Test
    @DisabledForJreRange(min = JRE.JAVA_16)
    fun `outdated version`() {
        val result = gradleRunner(project, emptyList(), GradleVersion.version("6.7")).buildAndFail()

        result.output shouldContain "Minimum supported Gradle version is 6.8. Current version is 6.7."
    }
}
