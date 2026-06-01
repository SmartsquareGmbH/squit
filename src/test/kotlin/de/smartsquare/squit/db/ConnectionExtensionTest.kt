package de.smartsquare.squit.db

import de.smartsquare.squit.TestUtils.getResourcePath
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
            val context = collection.createOrGet("jdbc:h2:mem:extensionTestDb", username, password)

            context.executeScript(sqlPath)

            val count = context.fetchCount(context.selectFrom("ANIMALS"))

            count shouldBeEqualTo 3
        }
    }

    @Test
    fun `executing dialect-specific sql does not throw`() {
        val sqlPath = getResourcePath("db_dialect.sql")

        ConnectionCollection().use { collection ->
            val context = collection.createOrGet("jdbc:h2:mem:extensionDialectTestDb", username, password)

            context.executeScript(sqlPath)

            val count = context.fetchCount(context.selectFrom("DIALECT_ANIMALS"))

            count shouldBeEqualTo 2
        }
    }
}
