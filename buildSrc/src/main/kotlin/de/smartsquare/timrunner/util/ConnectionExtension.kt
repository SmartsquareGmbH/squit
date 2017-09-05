@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

inline fun Connection.executeScript(path: Path) = createStatement().use { statement ->
    statement.execute(Files.readAllLines(path).joinToString { it.trim().trimEnd(';') })
}
