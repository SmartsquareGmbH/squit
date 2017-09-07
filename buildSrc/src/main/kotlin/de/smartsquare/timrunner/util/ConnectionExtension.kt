@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.timrunner.util

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

inline fun Connection.executeScript(path: Path) {
    try {
        createStatement().use { statement ->
            Files.readAllLines(path).joinToString(" ")
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { statement.addBatch(it) }

            statement.executeBatch()
        }

        commit()
    } catch (error: Throwable) {
        rollback()

        throw error
    }
}
