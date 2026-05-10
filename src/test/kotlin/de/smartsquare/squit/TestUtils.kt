package de.smartsquare.squit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.util.GradleVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.h2.Driver as H2Driver

/**
 * Utility class for methods which cannot be a extension function.
 */
object TestUtils {

    /**
     * Deletes all database files found in the given [path] directory.
     */
    fun deleteDatabaseFiles(path: Path) = Files
        .newDirectoryStream(path) { Files.isRegularFile(it) && it.fileName.toString().endsWith(".db") }
        .use { files -> files.forEach { Files.delete(it) } }

    /**
     * Returns the [Path] of the resource with the given [name]…
     */
    fun getResourcePath(name: String): Path = File(this.javaClass.classLoader.getResource(name)!!.toURI()).toPath()
}

/**
 * Adds the plugin classpath to this runner with additional needed dependencies, not being included by default.
 */
fun GradleRunner.withExtendedPluginClasspath(version: GradleVersion? = null): GradleRunner {
    val classpath = PluginUnderTestMetadataReading.readImplementationClasspath()

    classpath += listOf(
        File(XmlPreProcessor::class.java.protectionDomain.codeSource.location.toURI()),
        File(XmlPostProcessor::class.java.protectionDomain.codeSource.location.toURI()),
        File(JsonPreProcessor::class.java.protectionDomain.codeSource.location.toURI()),
        File(JsonPostProcessor::class.java.protectionDomain.codeSource.location.toURI()),
        File(JsonArrayPreProcessor::class.java.protectionDomain.codeSource.location.toURI()),
        File(JsonArrayPostProcessor::class.java.protectionDomain.codeSource.location.toURI()),
    )

    // Workaround a bug in older versions of Gradle that fail when reading JDK 21 files from the H2Driver jar.
    if (version == null || version >= GradleVersion.version("9.0")) {
        classpath += File(H2Driver::class.java.protectionDomain.codeSource.location.toURI())
    }

    return withPluginClasspath(classpath)
}

/**
 * Creates a [GradleRunner] with the given [project], [arguments] and an optional [version].
 *
 * This also applies the following default arguments: "clean --stacktrace".
 */
fun gradleRunner(project: Path, arguments: List<String>, version: GradleVersion? = null): GradleRunner =
    GradleRunner.create()
        .apply { if (version !== null) withGradleVersion(version.version) }
        .withArguments(listOf("clean") + arguments + "--stacktrace")
        .withExtendedPluginClasspath(version)
        .withProjectDir(project.toFile())
        .forwardOutput()
