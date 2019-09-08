package de.smartsquare.squit.mediatype.generic

import de.smartsquare.squit.mediatype.Canonicalizer

/**
 * Generic [Canonicalizer] implementation. Just returns the input String and applies no modifications.
 */
class GenericCanonicalizer : Canonicalizer {

    override fun canonicalize(input: String): String {
        return input
    }
}
