@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

inline fun Connection.executeScript(path: Path) {
    try {
        createStatement().use { statement ->
            Files.readAllBytes(path).toString(Charsets.UTF_8)
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace("\uFEFF", "")
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { statement.execute(it) }
        }

        commit()
    } catch (error: Throwable) {
        rollback()

        throw error
    }
}
