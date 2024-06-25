package de.smartsquare.squit.mediatype.generic

import de.smartsquare.squit.mediatype.Canonicalizer
import de.smartsquare.squit.mediatype.MediaTypeConfig

/**
 * Generic [Canonicalizer] implementation. Just returns the input String and applies no modifications.
 */
class GenericCanonicalizer : Canonicalizer {

    override fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String = input
}
