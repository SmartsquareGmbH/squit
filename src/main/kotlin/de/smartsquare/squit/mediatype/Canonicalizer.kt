package de.smartsquare.squit.mediatype

import de.smartsquare.squit.SquitExtension

/**
 * Interface for canonicalizing structures to make them easier to diff.
 */
interface Canonicalizer {

    /**
     * Canonicalize the [input] into a common format. E.g. for xml this might mean to remove all unnecessary whitespace
     * and order attributes.
     */
    fun canonicalize(input: String, extension: SquitExtension): String
}
