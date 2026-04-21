package de.smartsquare.squit.db

import de.smartsquare.squit.io.FilesUtils
import org.jooq.DSLContext
import java.nio.file.Path

/**
 * Executes the sql script at the given [path] using jOOQ's SQL parser to correctly split statements.
 */
fun DSLContext.executeScript(path: Path) {
    val sql = FilesUtils.readAllBytes(path).toString(Charsets.UTF_8)

    parser().parse(sql).forEach { it.execute() }
}
