@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import org.apache.poi.ss.usermodel.Row
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * Removes pieces of a path, based on another path. This is useful for getting the sub directories of a path, with the
 * same ancestors as another path.
 *
 * Given this is /a/b/c/d/e and [other] is /a/b/c, /d/e would be returned.
 */
inline fun Path.cut(other: Path): Path = this.subtract(other).reduce { current, path -> current.resolve(path) }

/**
 * Safely loads the properties for this from the given [path].
 *
 * Safe means in this context that the file is correctly closed.
 */
inline fun Properties.safeLoad(path: Path) = Files.newBufferedReader(path).use {
    this.apply { load(it) }
}

/**
 * Safely stores this properties at the given [path].
 *
 * Safe means in this context that the file is correctly closed.
 */
inline fun Properties.safeStore(path: Path, comments: String? = null) = Files.newBufferedWriter(path).use {
    this.apply { store(it, comments) }
}

/**
 * Reads and returns a [Document] at the given [path].
 *
 * This is a safe operation, as such the file is correctly closed.
 */
inline fun SAXReader.read(path: Path): Document = try {
    Files.newInputStream(path).use {
        read(it)
    }
} catch (error: Throwable) {
    throw GradleException("Could not read xml file: $path ($error)")
}

/**
 * Writes this [Document] to the given [path], with the specified [outputFormat] (defaulting to the pretty
 * printing one).
 *
 * This is a safe operation, as such the file is correctly closed.
 */
inline fun Document.write(path: Path, outputFormat: OutputFormat = OutputFormat.createPrettyPrint()) {
    Files.newBufferedWriter(path).use {
        XMLWriter(it, outputFormat).write(document)
    }
}

/**
 * Retrieves and returns the [String] of this [Row] at the given [position]. The result is cleaned (by [clean]) and
 * errors converted to a null value prior to returning.
 */
inline fun Row.safeCleanedStringValueAt(position: Int): String? {
    return try {
        getCell(position)?.stringCellValue?.let {
            when (it.isBlank()) {
                true -> null
                false -> it.clean()
            }
        }
    } catch (ignored: Throwable) {
        null
    }
}

/**
 * Cleans this [String] by removing sql comments, newlines and blanks, followed by trimming ([trim]).
 */
inline fun String.clean() = this
        .replace(Regex("--.*?\n", DOT_MATCHES_ALL), "")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("\uFEFF", "") // This is a weird unicode blank character, present in some sql files.
        .trim()