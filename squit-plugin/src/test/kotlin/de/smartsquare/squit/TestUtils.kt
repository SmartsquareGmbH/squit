package de.smartsquare.squit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class for methods which cannot be a extension function.
 */
object TestUtils {

    /**
     * Deletes all database files found in the passed [path] directory.
     */
    fun deleteDatabaseFiles(path: Path) = Files.list(path)
        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".db") }
        .forEach { Files.delete(it) }
}

/**
 * Adds the plugin classpath to this runner with additional needed dependencies, not being included by default.
 */
fun GradleRunner.withExtendedPluginClasspath(): GradleRunner {
    val classpath = PluginUnderTestMetadataReading.readImplementationClasspath()
        .plus(File(org.h2.Driver::class.java.protectionDomain.codeSource.location.toURI()))

    return withPluginClasspath(classpath)
}

/**
 * Helper method for using the gradle test-kit with jacoco.
 */
fun GradleRunner.withJaCoCo(): GradleRunner {
    javaClass.classLoader
        .getResourceAsStream("testkit-gradle.properties")
        .copyTo(File(projectDir, "gradle.properties").outputStream())

    return this
}
