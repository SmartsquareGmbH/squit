package de.smartsquare.squit

import org.dom4j.Document

/**
 * Interface for implementation of a Squit pre processor.
 */
interface SquitPostProcessor {

    /**
     * Processes the given [actualResponse] in place. The passed [expectedResponse] is not written, but can be useful
     * for reference when modifying the [actualResponse].
     */
    fun process(actualResponse: Document, expectedResponse: Document)
}
