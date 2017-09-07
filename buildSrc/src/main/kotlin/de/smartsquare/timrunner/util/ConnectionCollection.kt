package de.smartsquare.timrunner.util

import java.sql.Connection
import java.sql.DriverManager

class ConnectionCollection : AutoCloseable {

    private val connections = hashMapOf<Triple<String, String, String>, Connection>()

    fun createOrGet(jdbc: String, username: String, password: String): Connection {
        return Triple(jdbc, username, password).let {
            connections[it].let { value ->
                value ?: DriverManager.getConnection(it.first, it.second, it.third).also { result: Connection ->
                    connections.put(it, result)
                }.also {
                    it.autoCommit = false

                    it.createStatement().execute("ALTER SESSION SET NLS_LANGUAGE=ENGLISH")
                }
            }
        }
    }

    override fun close() {
        connections.values.forEach { it.close() }
    }
}
