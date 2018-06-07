package de.smartsquare.squit.db

import java.sql.Connection
import java.sql.DriverManager

/**
 * Data structure for holding [java.sql.Connection] objects with auto close capabilities.
 *
 * The connections are hold in a HashMap with a key of the jdbc address, the username and the password.
 *
 * @author Ruben Gees
 */
class ConnectionCollection : AutoCloseable {

    private val connections = hashMapOf<Triple<String, String, String>, Connection>()

    /**
     * Creates a new [java.sql.Connection] or returns an existing one, based on the passed [jdbc] address, [username]
     * and [password].
     */
    fun createOrGet(jdbc: String, username: String, password: String): Connection {
        val key = Triple(jdbc, username, password)
        val result = connections[key] ?: DriverManager.getConnection(key.first, key.second, key.third).also {
            connections[key] = it
        }

        result.autoCommit = false

        return result
    }

    override fun close() {
        connections.values.forEach { it.close() }
    }
}
