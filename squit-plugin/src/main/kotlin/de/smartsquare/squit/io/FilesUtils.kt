package de.smartsquare.squit.io

import de.smartsquare.squit.util.cut
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Object with io related utility methods in the style of [java.nio.file.Files].
 *
 * @author Ruben Gees
 */
object FilesUtils {

    /**
     * Returns all leaf directories of the given [path], sorted by alphanumeric order.
     */
    fun getSortedLeafDirectories(path: Path): List<Path> = getChildDirectories(path)
            .sortedWith(Comparator { first, second ->
                val firstNumber = first.fileName.toString().substringBefore("-").toIntOrNull()
                val secondNumber = second.fileName.toString().substringBefore("-").toIntOrNull()

                if (firstNumber != null) {
                    if (secondNumber != null) {
                        firstNumber.compareTo(secondNumber)
                    } else {
                        -1
                    }
                } else {
                    if (secondNumber != null) {
                        1
                    } else {
                        first.fileName.compareTo(second.fileName)
                    }
                }
            })
            .fold(listOf(), { current, it ->
                current + when (containsDirectories(it)) {
                    true -> getSortedLeafDirectories(it)
                    false -> listOf(it)
                }
            })

    /**
     * Deletes the given [path] recursively, if existing.
     */
    fun deleteRecursivelyIfExisting(path: Path) = when {
        Files.exists(path) -> Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
        else -> Unit
    }

    /**
     * Copies all files from the given [source] directory to the given [dest] directory.
     *
     * This is not recursive.
     */
    fun copyFilesFromDirectory(source: Path, dest: Path) {
        Files.newDirectoryStream(source, { Files.isRegularFile(it) }).use { files ->
            files.forEach { file -> Files.copy(file, dest.resolve(file.cut(source))) }
        }
    }

    /**
     * Validates if a file or directory exists at the given [path] and returns it.
     */
    @Throws(GradleException::class)
    fun validateExistence(path: Path): Path = when (Files.exists(path)) {
        true -> path
        false -> throw GradleException("Missing expected file: $path")
    }

    /**
     * Copies a resource specified by the passed [name] to the given [target] path.
     */
    fun copyResource(name: String, target: Path, modification: (ByteArray) -> ByteArray = { it }) {
        val resource = javaClass.classLoader.getResource(name).readBytes()

        Files.createDirectories(target.parent)
        Files.write(target, modification(resource))
    }

    private fun containsDirectories(path: Path) = Files.list(path).use {
        it.anyMatch { current -> Files.isDirectory(current) }
    }

    private fun getChildDirectories(path: Path) = Files.newDirectoryStream(path, { Files.isDirectory(it) })
            .use { it.toList() }
}
