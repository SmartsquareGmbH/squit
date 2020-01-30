package de.smartsquare.squit

import de.smartsquare.squit.interfaces.SquitXmlPreProcessor
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node

class XmlPreProcessor : SquitXmlPreProcessor {

    override fun process(request: Document?, expectedResponse: Document) {
        request?.selectNodes("//animal")?.forEach { node: Node ->
            (node as Element).addAttribute("pre", "true")
        }
    }
}
