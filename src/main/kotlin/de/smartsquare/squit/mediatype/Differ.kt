package de.smartsquare.squit.mediatype

/**
 * Interface for generating diffs between expected and actual responses.
 *
 * @author Ruben Gees
 */
interface Differ {

    /**
     * Calculates and return a user-friendly difference between the given [expectedResponse] and [actualResponse].
     */
    fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String
}
