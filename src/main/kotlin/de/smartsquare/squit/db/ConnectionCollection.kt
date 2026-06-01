package de.smartsquare.squit.db

import org.jooq.CloseableDSLContext
import org.jooq.DSLContext
import org.jooq.conf.ParseUnsupportedSyntax
import org.jooq.conf.Settings
import org.jooq.impl.DSL

/**
 * Data structure for holding [DSLContext] objects with auto close capabilities.
 *
 * The contexts are held in a HashMap keyed by the jdbc address, username and password triple.
 */
class ConnectionCollection : AutoCloseable {

    private val settings = Settings().withParseUnsupportedSyntax(ParseUnsupportedSyntax.IGNORE)
    private val contexts = mutableMapOf<Triple<String, String, String>, CloseableDSLContext>()

    /**
     * Creates a new [DSLContext] or returns an existing one, based on the passed [jdbc] address, [username]
     * and [password].
     */
    fun createOrGet(jdbc: String, username: String, password: String): DSLContext {
        val key = Triple(jdbc, username, password)

        return contexts.getOrPut(key) {
            DSL.using(jdbc, username, password).apply { configuration().set(settings) }
        }
    }

    override fun close() {
        var thrown: Throwable? = null

        for (context in contexts.values) {
            try {
                context.close()
            } catch (e: Exception) {
                if (thrown == null) thrown = e else thrown.addSuppressed(e)
            }
        }

        if (thrown != null) {
            throw thrown
        }
    }
}
