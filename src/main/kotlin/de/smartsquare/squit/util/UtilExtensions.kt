package de.smartsquare.squit.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import de.smartsquare.squit.entity.SquitOutputFormat
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
fun Path.cut(other: Path): Path {
    var mutableOther = other.toList()

    return this.toMutableList()
        .dropWhile {
            val isSame = it.fileName == mutableOther.firstOrNull()?.fileName ?: ""

            if (mutableOther.isNotEmpty()) {
                mutableOther = mutableOther.drop(1)
            }

            isSame
        }
        .fold(Paths.get("")) { acc, path -> acc.resolve(path) }
}

/**
 * Reads and returns a [org.dom4j.Document] at the given [path].
 *
 * This is a safe operation, as such the file is correctly closed.
 */
fun SAXReader.read(path: Path): XmlDocument = try {
    Files.newBufferedReader(path).use {
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
fun XmlDocument.write(path: Path, outputFormat: OutputFormat = SquitOutputFormat) {
    Files.newBufferedWriter(path).use {
        XMLWriter(it, outputFormat).write(document)
    }
}

/**
 * Reads and returns a [JsonElement] at the given [path].
 *
 * This is a safe operation, as such the file is correctly closed.
 */
fun JsonParser.read(path: Path): JsonElement = try {
    val jsonElement = Files.newBufferedReader(path).use {
        parse(it)
    }

    jsonElement
} catch (error: IOException) {
    throw GradleException("Could not read json file: $path ($error)")
} catch (error: JsonParseException) {
    throw GradleException("Could not read json file: $path ($error)")
}

/**
 * Writes this [JsonElement] to the given [path], with the specified [gson] instance
 * (defaulting to a pretty printing one).
 *
 * This is a safe operation, as such the file is correctly closed.
 */
fun JsonElement.write(path: Path, gson: Gson = GsonBuilder().setPrettyPrinting().create()) {
    Files.newBufferedWriter(path).use {
        gson.toJson(this, it)
    }
}

/**
 * Cleans this [String] by removing sql comments, newlines and blanks, followed by trimming ([trim]).
 */
fun String.cleanSqlString() = this
    .replace(Regex("--.*?\n", DOT_MATCHES_ALL), "")
    .replace("\n", " ")
    .replace("\r", " ")
    .replace("\uFEFF", "") // This is a weird unicode blank character, present in some sql files.
    .trim()

/**
 * Prints the given [message] to the standard output stream and flushes it afterwards.
 */
fun printAndFlush(message: Any?) {
    System.out.print(message)
    System.out.flush()
}
