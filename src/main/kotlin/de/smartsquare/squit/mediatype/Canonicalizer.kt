package de.smartsquare.squit.mediatype

/**
 * Interface for canonicalizing structures to make them easier to diff.
 *
 * @author Ruben Gees
 */
interface Canonicalizer {

    /**
     * Canonicalize the [input] into a common format. E.g. for xml this might mean to remove all unnecessary whitespace
     * and order attributes.
     */
    fun canonicalize(input: String): String
}
