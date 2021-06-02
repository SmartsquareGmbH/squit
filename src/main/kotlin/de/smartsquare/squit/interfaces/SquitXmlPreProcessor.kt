package de.smartsquare.squit.interfaces

import com.typesafe.config.Config
import org.dom4j.Document

/**
 * Interface for implementation of a Squit pre-processor.
 */
interface SquitXmlPreProcessor {

    /**
     * Processes the given [request] in place. The passed [expectedResponse] is not written, but can be useful for
     * reference when modifying the [request].
     */
    @Deprecated(
        message = "Use variant with config instead",
        replaceWith = ReplaceWith(
            "process(request, expectedResponse, Config)",
            imports = ["com.typesafe.config.Config"]
        )
    )
    @JvmDefault
    fun process(request: Document?, expectedResponse: Document) = Unit

    /**
     * Overloaded [process] with [config]. Users should only override one variant.
     */
    @Suppress("DEPRECATION")
    @JvmDefault
    fun process(request: Document?, expectedResponse: Document, config: Config) {
        process(request, expectedResponse)
    }
}
