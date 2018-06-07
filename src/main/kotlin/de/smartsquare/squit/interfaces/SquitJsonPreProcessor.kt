package de.smartsquare.squit.interfaces

import com.google.gson.JsonElement

/**
 * Interface for implementation of a Squit pre-processor.
 */
interface SquitJsonPreProcessor {

    /**
     * Processes the given [request] in place. The passed [expectedResponse] is not written, but can be useful for
     * reference when modifying the [request].
     */
    fun process(request: JsonElement?, expectedResponse: JsonElement)
}
