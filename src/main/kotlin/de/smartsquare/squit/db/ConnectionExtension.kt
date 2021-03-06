package de.smartsquare.squit.db

import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.cleanSqlString
import java.nio.file.Path
import java.sql.Connection
import java.sql.SQLException

/**
 * Executes the sql script at the given [path]. The contained statements are split by ";" and cleaned.
 */
fun Connection.executeScript(path: Path) {
    try {
        createStatement().use { statement ->
            FilesUtils.readAllBytes(path).toString(Charsets.UTF_8).cleanSqlString()
                .split(";")
                .map { it.cleanSqlString() }
                .filter { it.isNotBlank() }
                .forEach { statement.execute(it) }
        }

        commit()
    } catch (error: SQLException) {
        rollback()

        throw error
    }
}
