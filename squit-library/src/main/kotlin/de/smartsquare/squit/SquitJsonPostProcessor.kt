package de.smartsquare.squit

import com.google.gson.JsonElement

/**
 * Interface for implementation of a Squit post-processor.
 */
interface SquitJsonPostProcessor {

    /**
     * Processes the given [actualResponse] in place. The passed [expectedResponse] is not written, but can be useful
     * for reference when modifying the [actualResponse].
     */
    fun process(actualResponse: JsonElement, expectedResponse: JsonElement)
}
