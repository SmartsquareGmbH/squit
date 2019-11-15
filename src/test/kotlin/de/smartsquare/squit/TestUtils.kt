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

    return withPluginClasspath(classpath)
}
