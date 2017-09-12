package de.smartsquare.timrunner.db

import java.sql.Connection
import java.sql.DriverManager

/**
 * Data structure for holding [java.sql.Connection] objects with auto close capabilities.
 *
 * The connections are hold in a HashMap with a key of the jdbc address, the username and the password.
 */
class ConnectionCollection : AutoCloseable {

    private val connections = hashMapOf<Triple<String, String, String>, Connection>()

    /**
     * Creates a new [java.sql.Connection] or returns an existing one, based on the passed [jdbc] address, [username]
     * and [password].
     *
     * Upon creation, required session parameters are set.
     */
    fun createOrGet(jdbc: String, username: String, password: String): Connection {
        return Triple(jdbc, username, password).let {
            connections[it].let { value ->
                value ?: DriverManager.getConnection(it.first, it.second, it.third).also { result: Connection ->
                    connections.put(it, result)
                }.also {
                    it.createStatement().execute("ALTER SESSION SET NLS_LANGUAGE=ENGLISH")
                    it.createStatement().execute("ALTER SESSION SET NLS_NUMERIC_CHARACTERS='.,'")

                    it.autoCommit = false
                }
            }
        }
    }

    override fun close() {
        connections.values.forEach { it.close() }
    }
}
