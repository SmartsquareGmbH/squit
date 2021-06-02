package de.smartsquare.squit

import com.typesafe.config.Config
import de.smartsquare.squit.interfaces.SquitXmlPostProcessor
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node

class XmlPostProcessorWithConfig : SquitXmlPostProcessor {

    override fun process(actualResponse: Document, expectedResponse: Document, config: Config) {
        actualResponse.selectNodes("//test")?.forEach { node: Node ->
            (node as Element).addAttribute("postConfig", config.getString("mediaType"))
        }
    }
}
