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
fun GradleRunner.withExtendedPluginClasspath(): GradleRunner {
    val classpath = PluginUnderTestMetadataReading.readImplementationClasspath()
        .plus(File(H2Driver::class.java.protectionDomain.codeSource.location.toURI()))
        .plus(File(XmlPreProcessor::class.java.protectionDomain.codeSource.location.toURI()))
        .plus(File(XmlPostProcessor::class.java.protectionDomain.codeSource.location.toURI()))

    return withPluginClasspath(classpath)
}

/**
 * Creates a [GradleRunner] with the given [project], [arguments] and an optional [version].
 *
 * This also applies the following default arguments: "clean --stacktrace".
 */
fun gradleRunner(project: Path, arguments: List<String>, version: GradleVersion? = null): GradleRunner =
    GradleRunner.create()
        .withArguments(listOf("clean") + arguments + "--stacktrace")
        .apply { if (version !== null) withGradleVersion(version.version) }
        .withProjectDir(project.toFile())
        .withExtendedPluginClasspath()
        .withDebug(true)
        .forwardOutput()
