@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.squit.util

import com.google.gson.Gson
import de.smartsquare.squit.entity.SquitOutputFormat
import groovy.lang.MissingPropertyException
import groovy.text.SimpleTemplateEngine
import nu.studer.java.util.OrderedProperties
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.GradleException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import org.dom4j.Document as XmlDocument

/**
 * Removes pieces of a path, based on another path. This is useful for getting the sub directories of a path, with the
 * same ancestors as another path.
 *
 * Given this is /a/b/c/d/e and [other] is /a/b/c, /d/e would be returned.
 */
inline fun Path.cut(other: Path): Path {
    var mutableOther = other.toList()

    return this.toMutableList()
            .dropWhile {
                val isSame = it.fileName == mutableOther.firstOrNull()?.fileName ?: ""

                if (mutableOther.isNotEmpty()) {
                    mutableOther = mutableOther.drop(1)
                }

                isSame
            }
            .fold(Paths.get(""), { acc, path -> acc.resolve(path) })
}

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
@Suppress("ExpressionBodySyntax")
inline fun OrderedProperties.getTemplateProperty(key: String, templateProperties: Map<String, *>? = null): String? {
    return getProperty(key)?.let {
        when (templateProperties != null) {
            true -> try {
                SimpleTemplateEngine().createTemplate(it).make(templateProperties).toString()
            } catch (error: MissingPropertyException) {
                throw GradleException("Missing property \"${error.property}\" for template, did you forget " +
                        "to pass it with -P?", error)
            }
            false -> it
        }
    }
}

/**
 * Savely reads the json file ate the given [path] and returns an instance of the given [klass].
 *
 * Safe means in this context that the file is correctly closed.
 */
inline fun <T> Gson.fromSafeJson(path: Path, klass: Class<T>): T = Files.newBufferedReader(path).use {
    fromJson(it, klass)
}

/**
 * Reads and returns a [org.dom4j.Document] at the given [path].
 *
 * This is a safe operation, as such the file is correctly closed.
 */
inline fun SAXReader.read(path: Path): XmlDocument = try {
    Files.newInputStream(path).use {
        read(it)
    }
} catch (error: IOException) {
    throw GradleException("Could not read xml file: $path ($error)")
}

/**
 * Writes this [org.dom4j.Document] to the given [path], with the specified [outputFormat] (defaulting to the pretty
 * printing one without text padding).
 *
 * This is a safe operation, as such the file is correctly closed.
 */
inline fun XmlDocument.write(path: Path, outputFormat: OutputFormat = SquitOutputFormat) {
    Files.newBufferedWriter(path).use {
        XMLWriter(it, outputFormat).write(document)
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
