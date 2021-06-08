package de.smartsquare.squit.interfaces

import com.google.gson.JsonElement
import com.typesafe.config.Config

/**
 * Interface for implementation of a Squit pre-processor.
 */
interface SquitJsonPreProcessor {

    /**
     * Processes the given [request] and [expectedResponse] in place.
     */
    fun process(request: JsonElement?, expectedResponse: JsonElement, config: Config)
}
