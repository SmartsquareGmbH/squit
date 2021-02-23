package de.smartsquare.squit.mediatype

import org.gradle.api.tasks.Input

/**
 * Data class containing options for the media types.
 *
 * @property xmlStrict If the xml diffing should use strict (e.g. identic) comparison.
 * @property xmlCanonicalize If the html report should be canonicalized for xml tests.
 * @property jsonCanonicalize If the html report should be canonicalized for json tests.
 */
data class MediaTypeConfig(
    @get:Input val xmlStrict: Boolean = true,
    @get:Input val xmlCanonicalize: Boolean = true,
    @get:Input val jsonCanonicalize: Boolean = true,
    @get:Input val resolveInvalidNamespaces: Boolean = false,
    @get:Input val resolveNamespaceString: String = "http://"
)
