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
    @Deprecated(
        message = "Use variant with config instead",
        replaceWith = ReplaceWith(
            "process(actualResponse, expectedResponse, Config)",
            imports = ["com.typesafe.config.Config"]
        )
    )
    @JvmDefault
    fun process(actualResponse: JsonElement, expectedResponse: JsonElement) = Unit

    /**
     * Overloaded [process] with [config]. Users should only override one variant.
     */
    @Suppress("DEPRECATION")
    @JvmDefault
    fun process(actualResponse: JsonElement, expectedResponse: JsonElement, config: Config) {
        process(actualResponse, expectedResponse)
    }
}
