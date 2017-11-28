@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.squit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
 * Helper method which returns the [Path] of a resource, found by the specified [name].
 */
inline fun Any.getResource(name: String): Path = Paths.get(this.javaClass.classLoader.getResource(name).toURI())

/**
 * Helper method for adding the testImplementation classpath to the gradle test-kit runner.
 */
@Suppress("ExpressionBodySyntax")
inline fun GradleRunner.withTestClasspath(): GradleRunner {
    val classpath = mutableSetOf<File>()

    classpath.addAll(PluginUnderTestMetadataReading.readImplementationClasspath())
    classpath.addAll((javaClass.classLoader as URLClassLoader).urLs.map { File(it.file) })

    return withPluginClasspath(classpath)
}

/**
 * Helper method for using the gradle test-kit with jacoco.
 */
inline fun GradleRunner.withJaCoCo(): GradleRunner {
    javaClass.classLoader
            .getResourceAsStream("testkit-gradle.properties")
            .copyTo(File(projectDir, "gradle.properties").outputStream())

    return this
}
