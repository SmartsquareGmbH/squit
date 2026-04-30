package de.smartsquare.squit.io

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
     * Returns a sequence yielding every path starting with the passed [path] and walking up to its
     * ancestors until the given [until] condition is met. The default is when the current path has
     * no parent.
     */
    fun walkUpwards(path: Path, until: (Path) -> Boolean = { it.parent == null }): Sequence<Path> =
        generateSequence(path) { it.parent }.takeWhile { !until(it) }

    /**
     * Returns a sequence yielding every path starting with the passed [path] until the given
     * [until] (inclusive) is reached.
     */
    fun walkUpwards(path: Path, until: Path) = walkUpwards(path) { it.endsWith(until.parent) }

    /**
     * Returns all leaf directories of the given [path], optionally sorted by alphanumeric order
     * (if [sort] is set to true, which is the default).
     *
     * The given [path] itself is never yielded; only descendant directories that have no further
     * subdirectories qualify as leaves.
     */
    fun getLeafDirectories(path: Path, sort: Boolean = true): Sequence<Path> = sequence {
        val children = getChildDirectories(path)

        val ordered = if (sort) {
            children.sortedWith(compareBy(AlphanumericComparator()) { it.fileName.toString() })
        } else {
            children
        }

        for (child in ordered) {
            val grandchildren = getChildDirectories(child)

            if (grandchildren.isEmpty()) {
                yield(child)
            } else {
                yieldAll(getLeafDirectories(child, sort))
            }
        }
    }

    /**
     * Deletes the given [path] recursively, if existing.
     */
    fun deleteRecursivelyIfExisting(path: Path) = when {
        Files.exists(path) -> Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }

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
                Files.copy(file, dest.resolve(source.relativize(file)), REPLACE_EXISTING)
            }
        }
    }

    /**
     * Checks whether the directory at the given [path] is empty.
     */
    fun isDirectoryEmpty(path: Path): Boolean =
        Files.newDirectoryStream(path).use { dirStream -> dirStream.none() }

    /**
     * Returns the given [path] if it exists or null otherwise.
     */
    fun ifExists(path: Path) = if (Files.exists(path)) path else null

    /**
     * Validates if a file or directory exists at the given [path] and returns it.
     */
    @Throws(GradleException::class)
    fun validateExistence(path: Path): Path =
        if (Files.exists(path)) path else throw GradleException("Missing expected file: $path")

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
    fun <T> useBufferedReader(path: Path, block: (BufferedReader) -> T): T = try {
        Files.newBufferedReader(path).use(block)
    } catch (error: MalformedInputException) {
        throw IOException("Error reading file $path. Squit expects UTF-8 encoded files only.", error)
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

    private fun getChildDirectories(path: Path): List<Path> = Files.newDirectoryStream(path) { Files.isDirectory(it) }
        .use { it.toList() }
}
