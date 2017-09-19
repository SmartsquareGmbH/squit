package de.smartsquare.squit

import org.dom4j.Document

/**
 * Interface for implementation of a Squit pre processor.
 */
interface SquitPreProcessor {

    /**
     * Processes the given [request] in place. The passed [expectedResponse] is not written, but can be useful for
     * reference when modifying the [request].
     */
    fun process(request: Document, expectedResponse: Document)
}