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
            val result = mutableListOf(Arguments.of(GradleVersion.current()))

            // This older Gradle version does not work on Java 20+.
            if (JavaVersion.current() < JavaVersion.VERSION_20) {
                result += Arguments.of(GradleVersion.version("8.0.2"))
            }

            // This older Gradle version does not work on Java 21+.
            if (JavaVersion.current() < JavaVersion.VERSION_21) {
                result += Arguments.of(GradleVersion.version("7.3.2"))
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
            "squitTest",
            "-Psquit.endpointPlaceholder=${server.url("/")}",
            "-Psquit.rootDir=$project",
        )

        val result = gradleRunner(project, arguments, gradleVersion).build()

        result.task(":squitTest")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_16)
    fun `outdated version`() {
        val result = gradleRunner(project, emptyList(), GradleVersion.version("7.2")).buildAndFail()

        result.output shouldContain "Minimum supported Gradle version is 7.3. Current version is 7.2."
    }
}
