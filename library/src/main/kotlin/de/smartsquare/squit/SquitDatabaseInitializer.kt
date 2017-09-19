package de.smartsquare.squit

import java.sql.Connection

/**
 * Interface for implementation of database initializations.
 */
interface SquitDatabaseInitializer {

    /**
     * Initializes the passed [connection]. This may involve running arbitrarily sql statements or other settings.
     */
    fun initialize(connection: Connection)
}
