package de.smartsquare.timrunner.entity

/**
 * Data class storing the configuration of a single database.
 *
 * @property name The assigned name of the configuration.
 * @property jdbcAddress The jdbc address, used to connect.
 * @property username The username to use when connecting.
 * @property password The password to use when connecting.
 *
 * @author Ruben Gees
 */
data class TimDatabaseConfiguration(val name: String, val jdbcAddress: String,
                                    val username: String, val password: String)
