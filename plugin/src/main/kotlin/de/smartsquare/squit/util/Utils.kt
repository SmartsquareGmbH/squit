package de.smartsquare.squit.util

import nu.studer.java.util.OrderedProperties

/**
 * Object with various utility methods.
 */
object Utils {

    /**
     * Constructs a new default instance of [OrderedProperties].
     */
    fun newProperties(): OrderedProperties = OrderedProperties.OrderedPropertiesBuilder()
            .withSuppressDateInComment(true)
            .build()
}
