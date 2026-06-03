package de.smartsquare.squit.db

import de.smartsquare.squit.io.FilesUtils
import java.nio.file.Path
import java.sql.Connection

private const val STATEMENT_SEPARATOR = ';'
private const val LINE_COMMENT = "--"
private const val BLOCK_COMMENT_START = "/*"
private const val BLOCK_COMMENT_END = "*/"

/**
 * Executes the SQL script at the given [path] using the SQL parser to correctly split statements.
 */
fun Connection.executeScript(path: Path) {
    val sql = FilesUtils.readAllBytes(path).toString(Charsets.UTF_8)

    for (statement in splitSqlScript(sql)) {
        prepareStatement(statement).use { it.execute() }
    }
}

/**
 * Splits the given SQL [script] into individual statements.
 *
 * Honors single- and double-quoted string literals (with MySQL-style backslash escapes inside
 * them), single-line `--` comments and `slash-star ... star-slash` block comments.
 * An unterminated block comment is treated as a comment to end of input (no error is thrown).
 *
 * Adapted from Spring's `ScriptUtils#splitSqlScript`.
 */
internal fun splitSqlScript(script: String): List<String> {
    val statements = mutableListOf<String>()
    val current = StringBuilder()

    fun flushStatement() {
        val statement = current.toString().trim()
        if (statement.isNotEmpty()) statements.add(statement)
        current.setLength(0)
    }

    tailrec fun scan(i: Int): Unit = when {
        i >= script.length -> Unit

        script.startsWith(LINE_COMMENT, i) -> {
            val eol = script.indexOf('\n', i)
            if (eol >= 0) scan(eol + 1) else Unit
        }

        script.startsWith(BLOCK_COMMENT_START, i) -> {
            val end = script.indexOf(BLOCK_COMMENT_END, i + BLOCK_COMMENT_START.length)
            if (end >= 0) scan(end + BLOCK_COMMENT_END.length) else Unit
        }

        script[i] == '\'' || script[i] == '"' -> {
            val end = script.endOfStringLiteral(i)
            current.append(script, i, end)
            scan(end)
        }

        script[i] == STATEMENT_SEPARATOR -> {
            flushStatement()
            scan(i + 1)
        }

        else -> {
            current.append(script[i])
            scan(i + 1)
        }
    }

    scan(0)
    flushStatement()

    return statements
}

/**
 * Returns the index just past the end of the SQL string literal that starts at [start].
 *
 * Treats `\X` as an escape pair (MySQL-style) so an escaped quote does not terminate the literal.
 * If the literal is unterminated, returns [String.length].
 */
private tailrec fun String.endOfStringLiteral(start: Int, j: Int = start + 1): Int = when {
    j >= length -> length
    this[j] == '\\' -> endOfStringLiteral(start, j + 2)
    this[j] == this[start] -> j + 1
    else -> endOfStringLiteral(start, j + 1)
}
