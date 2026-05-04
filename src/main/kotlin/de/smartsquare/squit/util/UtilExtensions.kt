package de.smartsquare.squit.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.smartsquare.squit.entity.SquitOutputFormat
import de.smartsquare.squit.entity.SquitResult
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.nio.file.Files
import java.nio.file.Path
import org.dom4j.Document as XmlDocument

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
fun JsonElement.write(path: Path, gsonInstance: Gson = prettyGson) {
    Files.newBufferedWriter(path).use {
        gsonInstance.toJson(this, it)
    }
}

/**
 * Resolves one or more directory parts.
 */
fun DirectoryProperty.dir(first: String, vararg more: String): Provider<Directory> =
    dir(Path.of(first, *more).toString())

/**
 * Resolves one or more file parts.
 */
fun DirectoryProperty.file(first: String, vararg more: String): Provider<RegularFile> =
    file(Path.of(first, *more).toString())

/**
 * Returns this as a [Path].
 */
val DirectoryProperty.asPath: Path get() = asFile.get().toPath()

/**
 * Returns this as a [Path].
 */
val Provider<out FileSystemLocation>.asPath: Path get() = get().asFile.toPath()

/** Counts of successful, failed and ignored test results. */
data class TestResultCounts(val successful: Int, val failed: Int, val ignored: Int)

/**
 * Iterate the list of [SquitResult]s and returns a [TestResultCounts] of successful, failed and ignored tests.
 */
fun List<SquitResult>.countTestResults(): TestResultCounts {
    val successfulTests = count { !it.isIgnored && it.isSuccess }
    val failedTests = count { !it.isIgnored && !it.isSuccess }
    val ignoredTests = count { it.isIgnored }

    return TestResultCounts(successfulTests, failedTests, ignoredTests)
}

private val methodsRequiringRequestBody = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
private val methodsForbiddingRequestBody = setOf("GET", "HEAD")

/**
 * Returns whether the given HTTP [method] requires a request body.
 */
fun requiresRequestBody(method: String) = method in methodsRequiringRequestBody

/**
 * Returns whether the given HTTP [method] permits a request body.
 */
fun permitsRequestBody(method: String) = method !in methodsForbiddingRequestBody

/**
 * Prints the given [message] to the standard output stream and flushes it afterwards.
 */
fun printAndFlush(message: Any?) {
    print(message)
    System.out.flush()
}
