package de.smartsquare.squit.db

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ConnectionCollectionSpek : Spek({

    val jdbc = "jdbc:h2:mem:testDb"
    val username = "test"
    val password = "test"

    given("a connection collection") {
        val connectionCollection = ConnectionCollection()

        on("creating a new db connection") {
            val connection = connectionCollection.createOrGet(jdbc, username, password)

            it("should be a valid connection") {
                connection.isClosed.shouldBeFalse()
            }
        }

        on("getting an existing connection") {
            val connection = connectionCollection.createOrGet(jdbc, username, password)
            val connection2 = connectionCollection.createOrGet(jdbc, username, password)

            it("should return the exact same one") {
                connection shouldBe connection2
            }
        }

        on("closing the collection") {
            val connection = connectionCollection.createOrGet(jdbc, username, password)

            connectionCollection.close()

            it("should close all connections") {
                connection.isClosed.shouldBeTrue()
            }
        }
    }
})
