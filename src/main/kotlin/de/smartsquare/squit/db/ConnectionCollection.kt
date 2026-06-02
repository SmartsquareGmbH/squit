package de.smartsquare.squit.db

import java.sql.Connection
import java.sql.DriverManager

/**
 * Data structure for holding [Connection] objects with auto close capabilities.
 *
 * The connections are held in a HashMap keyed by the jdbc address, username and password triple.
 */
class ConnectionCollection : AutoCloseable {

    private val connections = mutableMapOf<Triple<String, String, String>, Connection>()

    /**
     * Creates a new [Connection] or returns an existing one, based on the passed [jdbc] address, [username]
     * and [password].
     */
    fun createOrGet(jdbc: String, username: String, password: String): Connection {
        val key = Triple(jdbc, username, password)

        return connections.getOrPut(key) {
            DriverManager.getConnection(jdbc, username, password)
        }
    }

    override fun close() {
        var thrown: Throwable? = null

        for (connection in connections.values) {
            try {
                connection.close()
            } catch (e: Exception) {
                if (thrown == null) thrown = e else thrown.addSuppressed(e)
            }
        }

        if (thrown != null) {
            throw thrown
        }
    }
}
