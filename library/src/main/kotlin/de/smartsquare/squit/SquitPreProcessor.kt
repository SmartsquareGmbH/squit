package de.smartsquare.squit

import org.dom4j.Document

interface SquitPreProcessor {
    fun process(request: Document, expectedResponse: Document)
}
