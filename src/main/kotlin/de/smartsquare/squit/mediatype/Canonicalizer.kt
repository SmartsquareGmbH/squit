package de.smartsquare.squit.mediatype

/**
 * Interface for canonicalizing structures to make them easier to diff.
 */
interface Canonicalizer {

    /**
     * Canonicalize the [input] into a common format. E.g. for xml this might mean to remove all unnecessary whitespace
     * and order attributes.
     */
    fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String
}
