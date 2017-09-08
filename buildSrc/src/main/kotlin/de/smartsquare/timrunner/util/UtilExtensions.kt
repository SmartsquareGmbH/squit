@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import org.apache.poi.ss.usermodel.Row
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

inline fun Path.cut(other: Path) = this.subtract(other).reduce { current, path -> current.resolve(path) }

inline fun Properties.safeLoad(path: Path) = Files.newBufferedReader(path).use {
    this.apply { load(it) }
}

inline fun Properties.safeStore(path: Path, comments: String? = null) = Files.newBufferedWriter(path).use {
    this.apply { store(it, comments) }
}

inline fun SAXReader.read(path: Path) = Files.newInputStream(path).use {
    read(it)
}

inline fun Document.write(path: Path, outputFormat: OutputFormat = TimOutputFormat()) = Files.newBufferedWriter(path)
        .use { XMLWriter(it, outputFormat).write(document) }

inline fun Row.safeCleanedStringValueAt(position: Int): String? {
    return try {
        getCell(position)?.stringCellValue?.let {
            when (it.isBlank()) {
                true -> null
                false -> it.trim().replace("\n", "").replace("\r", "")
            }
        }
    } catch (ignored: Throwable) {
        null
    }
}