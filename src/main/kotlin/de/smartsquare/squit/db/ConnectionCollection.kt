package de.smartsquare.squit.db

import org.jooq.CloseableDSLContext
import org.jooq.DSLContext
import org.jooq.impl.DSL

/**
 * Data structure for holding [DSLContext] objects with auto close capabilities.
 *
 * The contexts are held in a HashMap keyed by the jdbc address, username and password triple.
 */
class ConnectionCollection : AutoCloseable {

    private val contexts = hashMapOf<Triple<String, String, String>, CloseableDSLContext>()

    /**
     * Creates a new [DSLContext] or returns an existing one, based on the passed [jdbc] address, [username]
     * and [password].
     */
    fun createOrGet(jdbc: String, username: String, password: String): DSLContext {
        val key = Triple(jdbc, username, password)

        return contexts.getOrPut(key) {
            DSL.using(jdbc, username, password)
        }
    }

    override fun close() {
        contexts.values.forEach { it.close() }
    }
}
