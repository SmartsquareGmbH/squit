package de.smartsquare.squit.interfaces

import com.google.gson.JsonElement
import com.typesafe.config.Config

/**
 * Interface for implementation of a Squit post-processor.
 */
interface SquitJsonPostProcessor {

    /**
     * Processes the given [actualResponse] in place. The passed [expectedResponse] is not written, but can be useful
     * for reference when modifying the [actualResponse].
     */
    fun process(actualResponse: JsonElement, expectedResponse: JsonElement, config: Config)
}
