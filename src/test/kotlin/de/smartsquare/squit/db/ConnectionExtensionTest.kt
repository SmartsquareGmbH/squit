package de.smartsquare.squit.db

import de.smartsquare.squit.TestUtils.getResourcePath
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.h2.Driver as H2Driver

class ConnectionExtensionTest {

    private val username = "test"
    private val password = "test"

    @BeforeEach
    fun setUp() {
        H2Driver.load()
    }

    @Test
    fun `executing sql with comments does not throw`() {
        val sqlPath = getResourcePath("db.sql")

        ConnectionCollection().use { collection ->
            val connection = collection.createOrGet("jdbc:h2:mem:extensionTestDb", username, password)

            connection.executeScript(sqlPath)

            val count = connection.prepareStatement("SELECT COUNT(*) FROM ANIMALS").executeQuery()
                .use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }

            count shouldBeEqualTo 3
        }
    }

    @Test
    fun `executing dialect-specific sql does not throw`() {
        val sqlPath = getResourcePath("db_dialect.sql")

        ConnectionCollection().use { collection ->
            val connection = collection.createOrGet("jdbc:h2:mem:extensionDialectTestDb", username, password)

            connection.executeScript(sqlPath)

            val count = connection.prepareStatement("SELECT COUNT(*) FROM DIALECT_ANIMALS").executeQuery()
                .use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }

            count shouldBeEqualTo 2
        }
    }

    @Test
    fun `splitting an empty script returns no statements`() {
        splitSqlScript("") shouldBeEqualTo emptyList()
        splitSqlScript("   \n\t  ") shouldBeEqualTo emptyList()
        splitSqlScript(";;;") shouldBeEqualTo emptyList()
    }

    @Test
    fun `splitting a single statement without trailing separator returns one statement`() {
        splitSqlScript("SELECT 1") shouldBeEqualTo listOf("SELECT 1")
    }

    @Test
    fun `splitting on semicolon yields multiple statements`() {
        val script = "INSERT INTO t VALUES (1); INSERT INTO t VALUES (2);"

        splitSqlScript(script) shouldBeEqualTo listOf(
            "INSERT INTO t VALUES (1)",
            "INSERT INTO t VALUES (2)",
        )
    }

    @Test
    fun `semicolons inside single-quoted strings do not split statements`() {
        val script = "INSERT INTO t VALUES ('a;b;c'); INSERT INTO t VALUES ('d');"

        splitSqlScript(script) shouldBeEqualTo listOf(
            "INSERT INTO t VALUES ('a;b;c')",
            "INSERT INTO t VALUES ('d')",
        )
    }

    @Test
    fun `semicolons inside double-quoted identifiers do not split statements`() {
        val script = """SELECT "a;b" FROM t; SELECT 1;"""

        splitSqlScript(script) shouldBeEqualTo listOf(
            """SELECT "a;b" FROM t""",
            "SELECT 1",
        )
    }

    @Test
    fun `single quotes inside double-quoted strings are not treated as quotes`() {
        val script = """SELECT "a'b; c" FROM t;"""

        splitSqlScript(script) shouldBeEqualTo listOf("""SELECT "a'b; c" FROM t""")
    }

    @Test
    fun `backslash escapes the following character inside a string literal`() {
        val script = """INSERT INTO t VALUES ('a\'b; c'); SELECT 1;"""

        splitSqlScript(script) shouldBeEqualTo listOf(
            """INSERT INTO t VALUES ('a\'b; c')""",
            "SELECT 1",
        )
    }

    @Test
    fun `line comments are stripped`() {
        val script = """
            -- leading comment
            SELECT 1; -- trailing comment
            SELECT 2;-- end of file comment
        """.trimIndent()

        splitSqlScript(script) shouldBeEqualTo listOf("SELECT 1", "SELECT 2")
    }

    @Test
    fun `block comments are stripped`() {
        val script = "SELECT /* inline */ 1 FROM dual; /* between */ SELECT 2;"

        splitSqlScript(script) shouldBeEqualTo listOf("SELECT  1 FROM dual", "SELECT 2")
    }

    @Test
    fun `multi-line block comments are stripped`() {
        val script = """
            /*
             * Comment
             */
            SELECT 1;
        """.trimIndent()

        splitSqlScript(script) shouldBeEqualTo listOf("SELECT 1")
    }

    @Test
    fun `comment delimiters inside string literals are preserved`() {
        val script = "SELECT '-- not a comment' FROM t; SELECT '/* also not */' FROM t;"

        splitSqlScript(script) shouldBeEqualTo listOf(
            "SELECT '-- not a comment' FROM t",
            "SELECT '/* also not */' FROM t",
        )
    }

    @Test
    fun `missing block comment end counts as never ending comment`() {
        val script = "/* unterminated SELECT 1"

        splitSqlScript(script).shouldBeEmpty()
    }

    @Test
    fun `escaped quotes are ignored`() {
        val script = """INSERT INTO t VALUES ('\'quoted');"""

        splitSqlScript(script) shouldBeEqualTo listOf("""INSERT INTO t VALUES ('\'quoted')""")
    }

    @Test
    fun `splitting preserves quoted whitespace verbatim`() {
        val script = "INSERT INTO t VALUES ('a   b\tc');"

        splitSqlScript(script) shouldBeEqualTo listOf("INSERT INTO t VALUES ('a   b\tc')")
    }
}
