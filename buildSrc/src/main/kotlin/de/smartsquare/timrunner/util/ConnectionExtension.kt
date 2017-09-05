@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

inline fun Connection.executeScript(path: Path) = Files.readAllLines(path).joinToString().split(";")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { createStatement().use { statement -> statement.execute(it) } }
