package de.smartsquare.squit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.util.GradleVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Utility class for methods which cannot be a extension function.
 */
object TestUtils {

    private val DB_FILTER = { it: Path ->
        Files.isRegularFile(it) && it.fileName.toString().endsWith(".db")
    }

    /**
     * Deletes all database files found in the passed [path] directory.
     */
    fun deleteDatabaseFiles(path: Path) = Files.newDirectoryStream(path, DB_FILTER).use { files ->
        files.forEach { Files.delete(it) }
    }

    /**
     * Returns the [Path] of the resource with the given [name]…
     */
    fun getResourcePath(name: String): Path = File(this.javaClass.classLoader.getResource(name)!!.toURI()).toPath()
}

/**
 * Adds the plugin classpath to this runner with additional needed dependencies, not being included by default.
 */
fun GradleRunner.withExtendedPluginClasspath(): GradleRunner {
    val classpath = PluginUnderTestMetadataReading.readImplementationClasspath()
        .plus(File(org.h2.Driver::class.java.protectionDomain.codeSource.location.toURI()))
        .plus(File(XmlPreProcessor::class.java.protectionDomain.codeSource.location.toURI()))
        .plus(File(XmlPostProcessor::class.java.protectionDomain.codeSource.location.toURI()))

    return withPluginClasspath(classpath)
}

/**
 * Configures the GradleRunner to run with the jacoco agent.
 */
fun GradleRunner.withJacoco(): GradleRunner {
    val properties = this.javaClass.classLoader.getResource("testkit-gradle.properties")

    if (properties != null) {
        Files.copy(
            File(properties.toURI()).toPath(),
            projectDir.toPath().resolve("gradle.properties"),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    return this
}

/**
 * Creates a [GradleRunner] with the given [project], [arguments] and an optional [version].
 *
 * This also applies the following default arguments: "clean --stacktrace".
 */
fun gradleRunner(project: Path, arguments: List<String>, version: GradleVersion? = null): GradleRunner {
    return GradleRunner.create()
        .withArguments(listOf("clean") + arguments + "--stacktrace")
        .apply { if (version !== null) withGradleVersion(version.version) }
        .withProjectDir(project.toFile())
        .withExtendedPluginClasspath()
        .forwardOutput()
        .withDebug(true)
        .withJacoco()
}
