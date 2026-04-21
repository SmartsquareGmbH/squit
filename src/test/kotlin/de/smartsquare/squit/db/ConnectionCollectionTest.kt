package de.smartsquare.squit.db

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import org.h2.Driver as H2Driver

class ConnectionCollectionTest {

    private val jdbc = "jdbc:h2:mem:testDb"
    private val username = "test"
    private val password = "test"

    @BeforeEach
    fun setUp() {
        H2Driver.load()
    }

    @Test
    fun `creating a new db connection`() {
        ConnectionCollection().use { collection ->
            val context = collection.createOrGet(jdbc, username, password)

            context.connection { it.isClosed.shouldBeFalse() }
        }
    }

    @Test
    fun `getting an existing connection`() {
        ConnectionCollection().use { collection ->
            val context = collection.createOrGet(jdbc, username, password)
            val context2 = collection.createOrGet(jdbc, username, password)

            context shouldBe context2
        }
    }

    @Test
    fun `closing the collection`() {
        var underlying: Connection? = null

        ConnectionCollection().use { collection ->
            val context = collection.createOrGet(jdbc, username, password)
            context.connection { underlying = it }
        }

        underlying!!.isClosed.shouldBeTrue()
    }
}
