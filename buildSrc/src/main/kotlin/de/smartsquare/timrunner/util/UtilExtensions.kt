@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import groovy.lang.MissingPropertyException
import groovy.text.SimpleTemplateEngine
import nu.studer.java.util.OrderedProperties
import org.apache.poi.ss.usermodel.Row
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
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
inline fun OrderedProperties.safeLoad(path: Path) = Files.newBufferedReader(path).use {
    this.apply { load(it) }
}

/**
 * Safely stores this properties at the given [path].
 *
 * Safe means in this context that the file is correctly closed.
 */
inline fun OrderedProperties.safeStore(path: Path, comments: String? = null) = Files.newBufferedWriter(path).use {
    this.apply { store(it, comments) }
}

/**
 * Returns the property with the given [key] after applying the given [templateProperties] if present on any present
 * templates in the property [String].
 */
inline fun OrderedProperties.getTemplateProperty(key: String, templateProperties: Map<String, *>? = null): String? {
    return getProperty(key)?.let {
        when (templateProperties != null) {
            true -> try {
                SimpleTemplateEngine().createTemplate(it).make(templateProperties).toString()
            } catch (error: MissingPropertyException) {
                throw GradleException("Missing property \"${error.property}\" for template, did you forget " +
                        "to pass it with -P?")
            }
            false -> it
        }
    }
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

/**
 * Prints the given [message] to the standard output stream and flushes it afterwards.
 */
inline fun printAndFlush(message: Any?) {
    System.out.print(message)
    System.out.flush()
}