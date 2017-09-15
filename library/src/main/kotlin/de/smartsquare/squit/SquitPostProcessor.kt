package de.smartsquare.squit

import org.dom4j.Document

interface SquitPostProcessor {
    fun process(actualResponse: Document, expectedResponse: Document)
}
