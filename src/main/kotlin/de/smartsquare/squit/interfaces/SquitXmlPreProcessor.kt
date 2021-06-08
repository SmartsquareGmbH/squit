package de.smartsquare.squit.interfaces

import com.typesafe.config.Config
import org.dom4j.Document

/**
 * Interface for implementation of a Squit pre-processor.
 */
interface SquitXmlPreProcessor {

    /**
     * Processes the given [request] and [expectedResponse] in place.
     */
    fun process(request: Document?, expectedResponse: Document, config: Config)
}
