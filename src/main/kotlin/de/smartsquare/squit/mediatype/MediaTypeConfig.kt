package de.smartsquare.squit.mediatype

import org.gradle.api.tasks.Input

/**
 * Data class containing options for the media types.
 *
 * @property xmlStrict If the XML diffing should use strict (e.g. identic) comparison.
 * @property xmlCanonicalize If the HTML report should be canonicalized for XML tests.
 * @property jsonCanonicalize If the HTML report should be canonicalized for JSON tests.
 */
data class MediaTypeConfig(
    @get:Input val xmlStrict: Boolean = true,
    @get:Input val xmlCanonicalize: Boolean = true,
    @get:Input val xmlResolveInvalidNamespaces: Boolean = false,
    @get:Input val jsonCanonicalize: Boolean = true,
)
