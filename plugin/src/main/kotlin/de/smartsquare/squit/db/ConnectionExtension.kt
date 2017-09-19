@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.squit.db

import de.smartsquare.squit.util.clean
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

/**
 * Executes the sql script at the given [path]. The contained statements are split by ";" and cleaned.
 *
 * @author Ruben Gees
 */
inline fun Connection.executeScript(path: Path) {
    try {
        createStatement().use { statement ->
            Files.readAllBytes(path).toString(Charsets.UTF_8).clean()
                    .split(";")
                    .map { it.clean() }
                    .filter { it.isNotBlank() }
                    .forEach { statement.execute(it) }
        }

        commit()
    } catch (error: Throwable) {
        rollback()

        throw error
    }
}