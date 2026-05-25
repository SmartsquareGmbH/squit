package de.smartsquare.squit.db

import de.smartsquare.squit.TestUtils.getResourcePath
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import org.h2.Driver as H2Driver

class ConnectionExtensionTest {

    private val jdbc = "jdbc:h2:mem:extensionTestDb"
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
            val context = collection.createOrGet(jdbc, username, password)

            context.executeScript(sqlPath)

            val count = context.fetchCount(context.selectFrom("ANIMALS"))

            count shouldBeEqualTo 3
        }
    }
}
