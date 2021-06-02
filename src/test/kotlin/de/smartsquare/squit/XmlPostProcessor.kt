package de.smartsquare.squit

import de.smartsquare.squit.interfaces.SquitXmlPostProcessor
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node

class XmlPostProcessor : SquitXmlPostProcessor {

    @Suppress("OverridingDeprecatedMember")
    override fun process(actualResponse: Document, expectedResponse: Document) {
        actualResponse.selectNodes("//test")?.forEach { node: Node ->
            (node as Element).addAttribute("post", "true")
        }
    }
}
