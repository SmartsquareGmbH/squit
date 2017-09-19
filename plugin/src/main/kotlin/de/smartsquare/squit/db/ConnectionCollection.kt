package de.smartsquare.squit.db

import de.smartsquare.squit.SquitDatabaseInitializer
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
     *
     * Upon creation, the [initializer] is invoked, if present.
     */
    fun createOrGet(jdbc: String, username: String, password: String,
                    initializer: SquitDatabaseInitializer? = null): Connection {
        return Triple(jdbc, username, password).let {
            connections[it].let { value ->
                value ?: DriverManager.getConnection(it.first, it.second, it.third).also { result: Connection ->
                    connections.put(it, result)
                }.also {
                    initializer?.initialize(it)

                    it.autoCommit = false
                }
            }
        }
    }

    override fun close() {
        connections.values.forEach { it.close() }
    }
}
