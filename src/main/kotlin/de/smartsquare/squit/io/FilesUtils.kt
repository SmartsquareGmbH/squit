package de.smartsquare.squit.io

import de.smartsquare.squit.util.cut
import org.gradle.api.GradleException
import se.sawano.java.text.AlphanumericComparator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Object with io related utility methods in the style of [java.nio.file.Files].
 */
object FilesUtils {

    /**
     * Returns a sequence yielding every path starting with the passed [path] until the given [until] condition is met.
     * The default is when parent is null.
     */
    fun walkUpwards(path: Path, until: (Path) -> Boolean = { path.parent == null }): Sequence<Path> = when {
        until(path) -> emptySequence()
        else -> sequence {
            yield(path)
            yieldAll(walkUpwards(path.parent, until))
        }
    }

    /**
     * Returns all leaf directories of the given [path], optionally sorted by alphanumeric order
     * (if [sort] is set to true, which is the default).
     */
    fun getLeafDirectories(path: Path, sort: Boolean = true): Sequence<Path> = getChildDirectories(path)
        .let { directories ->
            when (sort) {
                true -> directories.sortedWith(compareBy(AlphanumericComparator()) { it.fileName.toString() })
                false -> directories
            }
        }
        .fold(sequenceOf()) { current, next ->
            current + when (containsDirectories(next)) {
                true -> getLeafDirectories(next, sort)
                false -> sequenceOf(next)
            }
        }

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
        Files.newDirectoryStream(source) { Files.isRegularFile(it) }.use { files ->
            files.forEach { file ->
                Files.copy(file, dest.resolve(file.cut(source)), REPLACE_EXISTING)
            }
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
        val resource = requireNotNull(javaClass.classLoader.getResource(name)).readBytes()

        Files.createDirectories(target.parent)
        Files.write(target, modification(resource))
    }

    private fun containsDirectories(path: Path) = Files.newDirectoryStream(path) { Files.isDirectory(it) }
        .use { it.any() }

    private fun getChildDirectories(path: Path) = Files.newDirectoryStream(path) { Files.isDirectory(it) }
        .use { it.toList() }
}
