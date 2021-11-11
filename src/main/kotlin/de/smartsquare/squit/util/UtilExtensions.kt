package de.smartsquare.squit.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import de.smartsquare.squit.entity.SquitOutputFormat
import de.smartsquare.squit.entity.SquitResult
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.gradle.api.file.DirectoryProperty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import org.dom4j.Document as XmlDocument

private val sqlCommentRegex = Regex("--.*?\n", DOT_MATCHES_ALL)

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
            val isSame = it.fileName == (mutableOther.firstOrNull()?.fileName ?: "")

            if (mutableOther.isNotEmpty()) {
                mutableOther = mutableOther.drop(1)
            }

            isSame
        }
        .fold(Paths.get("")) { acc, path -> acc.resolve(path) }
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
    .replace(sqlCommentRegex, "")
    .replace("\n", " ")
    .replace("\r", " ")
    .replace("\uFEFF", "") // This is a weird unicode blank character, present in some sql files.
    .trim()

/**
 * Returns this as a [Path].
 */
val DirectoryProperty.asPath: Path get() = asFile.get().toPath()

/**
 * Iterate the list of [SquitResult]s and returns a [Triple] of successful, failed and ignored tests.
 */
fun List<SquitResult>.countTestResults(): Triple<Int, Int, Int> {
    val successfulTests = count { !it.isIgnored && it.isSuccess }
    val failedTests = count { !it.isIgnored && !it.isSuccess }
    val ignoredTests = count { it.isIgnored }

    return Triple(successfulTests, failedTests, ignoredTests)
}

/**
 * Prints the given [message] to the standard output stream and flushes it afterwards.
 */
fun printAndFlush(message: Any?) {
    print(message)
    System.out.flush()
}
