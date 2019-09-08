package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.entity.SquitOutputFormat
import de.smartsquare.squit.mediatype.Canonicalizer
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.StringWriter
import org.apache.xml.security.Init as ApacheInit
import org.apache.xml.security.c14n.Canonicalizer as ApacheCanonicalizer

/**
 * [Canonicalizer] for Xml.
 *
 * @author Ruben Gees
 */
class XmlCanonicalizer : Canonicalizer {

    init {
        ApacheInit.init()
    }

    override fun canonicalize(input: String): String {
        val canonicalizer = ApacheCanonicalizer.getInstance(ApacheCanonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS)
        val output = canonicalizer.canonicalize(input.toByteArray())

        return SAXReader().read(output.inputStream()).asString(OutputFormat.createPrettyPrint())
    }

    private fun Document.asString(outputFormat: OutputFormat = SquitOutputFormat): String {
        return StringWriter()
            .also { XMLWriter(it, outputFormat).write(this) }
            .toString()
    }
}
