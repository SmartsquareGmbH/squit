package de.smartsquare.squit.db

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ConnectionCollectionTest {

    private val jdbc = "jdbc:h2:mem:testDb"
    private val username = "test"
    private val password = "test"

    @Test
    fun `creating a new db connection`() {
        val connectionCollection = ConnectionCollection()
        val connection = connectionCollection.createOrGet(jdbc, username, password)

        connection.isClosed.shouldBeFalse()
    }

    @Test
    fun `getting an existing connection`() {
        val connectionCollection = ConnectionCollection()
        val connection = connectionCollection.createOrGet(jdbc, username, password)
        val connection2 = connectionCollection.createOrGet(jdbc, username, password)

        connection shouldBe connection2
    }

    @Test
    fun `closing the collection`() {
        val connectionCollection = ConnectionCollection()
        val connection = connectionCollection.createOrGet(jdbc, username, password)

        connectionCollection.close()

        connection.isClosed.shouldBeTrue()
    }
}
