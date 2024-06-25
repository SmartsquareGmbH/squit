package de.smartsquare.squit.entity

/**
 * Data class storing the configuration of a single database.
 *
 * @property name The assigned name of the configuration.
 * @property jdbcAddress The jdbc address, used to connect.
 * @property username The username to use when connecting.
 * @property password The password to use when connecting.
 */
data class SquitDatabaseConfiguration(
    val name: String,
    val jdbcAddress: String,
    val username: String,
    val password: String,
)
