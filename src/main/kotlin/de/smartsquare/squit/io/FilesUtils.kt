package de.smartsquare.squit.io

import de.smartsquare.squit.util.cut
import org.gradle.api.GradleException
import se.sawano.java.text.AlphanumericComparator
import java.io.BufferedReader
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Object with io related utility methods in the style of [java.nio.file.Files].
 */
@Suppress("TooManyFunctions")
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
     * Returns a sequence yielding every path starting with the passed [path] until the given
     * [until] (inclusive) is reached.
     */
    fun walkUpwards(path: Path, until: Path) = walkUpwards(path) { it.endsWith(until.parent) }

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
     * Checks whether the directory at the given [path] is empty.
     */
    fun isDirectoryEmpty(path: Path): Boolean {
        return Files.newDirectoryStream(path).use { dirStream -> !dirStream.iterator().hasNext() }
    }

    /**
     * Returns the given [path] if it exists or null otherwise.
     */
    fun ifExists(path: Path) = when (Files.exists(path)) {
        true -> path
        false -> null
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
        val resource = requireNotNull(javaClass.classLoader.getResource(name)) {
            "Could not find resource $name on classpath"
        }

        Files.createDirectories(target.parent)
        Files.write(target, modification(resource.readBytes()))
    }

    /**
     * Creates a new [BufferedReader] for the given [path] and executes [block] with improved error handling.
     */
    fun <T> useBufferedReader(path: Path, block: (BufferedReader) -> T): T {
        return try {
            Files.newBufferedReader(path).use(block)
        } catch (error: MalformedInputException) {
            throw IOException("Error reading file $path. Squit expects UTF-8 encoded files only.", error)
        }
    }

    /**
     * Reads all bytes at the given [path] with improved error handling.
     */
    fun readAllBytes(path: Path): ByteArray = try {
        Files.readAllBytes(path)
    } catch (error: MalformedInputException) {
        throw IOException("Error reading file $path. Squit expects UTF-8 encoded files only.", error)
    }

    /**
     * Reads all lines at the given [path] with improved error handling.
     */
    fun readAllLines(path: Path): List<String> = try {
        Files.readAllLines(path)
    } catch (error: MalformedInputException) {
        throw IOException("Error reading file $path. Squit expects UTF-8 encoded files only.", error)
    }

    private fun containsDirectories(path: Path) = Files.newDirectoryStream(path) { Files.isDirectory(it) }
        .use { it.any() }

    private fun getChildDirectories(path: Path) = Files.newDirectoryStream(path) { Files.isDirectory(it) }
        .use { it.toList() }
}
