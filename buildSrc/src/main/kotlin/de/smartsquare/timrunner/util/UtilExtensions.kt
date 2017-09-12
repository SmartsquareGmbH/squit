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

inline fun Path.cut(other: Path): Path = this.subtract(other).reduce { current, path -> current.resolve(path) }

inline fun Properties.safeLoad(path: Path) = Files.newBufferedReader(path).use {
    this.apply { load(it) }
}

inline fun Properties.safeStore(path: Path, comments: String? = null) = Files.newBufferedWriter(path).use {
    this.apply { store(it, comments) }
}

inline fun SAXReader.read(path: Path): Document = try {
    Files.newInputStream(path).use {
        read(it)
    }
} catch (error: Throwable) {
    throw GradleException("Could not read xml file: $path ($error)")
}

inline fun Document.write(path: Path, outputFormat: OutputFormat = TimOutputFormat()) = Files.newBufferedWriter(path)
        .use { XMLWriter(it, outputFormat).write(document) }

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

inline fun String.clean() = this
        .replace(Regex("--.*?\n", DOT_MATCHES_ALL), "")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("\uFEFF", "")
        .trim()