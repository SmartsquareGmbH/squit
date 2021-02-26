package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.entity.SquitOutputFormat
import de.smartsquare.squit.mediatype.Canonicalizer
import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import org.apache.xml.security.Init as ApacheInit
import org.apache.xml.security.c14n.Canonicalizer as ApacheCanonicalizer

/**
 * [Canonicalizer] for Xml.
 */
class XmlCanonicalizer : Canonicalizer {

    private val canonicalizer: org.apache.xml.security.c14n.Canonicalizer

    init {
        ApacheInit.init()

        canonicalizer = ApacheCanonicalizer.getInstance(ApacheCanonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS)
    }

    private companion object {
        private val xmlNamespaceRegex = Regex("(xmlns:\\w+=['\"])(.*?)(['\"])")
        private val urlRegex = Regex("^https?://")
        private const val resolveNamespaceString = "http://"
    }

    override fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String {
        return if (mediaTypeConfig.xmlCanonicalize) {
            val outputStream = ByteArrayOutputStream()

            val content = if (mediaTypeConfig.resolveInvalidNamespaces)
                resolveInvalidNamespaces(input, resolveNamespaceString)
            else
                input

            canonicalizer.canonicalize(content.toByteArray(), outputStream, false)

            SAXReader().read(outputStream.toByteArray().inputStream()).asString()
        } else {
            input
        }
    }

    private fun resolveInvalidNamespaces(content: String, resolveNamespaceString: String): String {
        return content.replace(xmlNamespaceRegex) { match ->
            val start = match.groupValues[1]
            val potentialUrlString = match.groupValues[2]
            val end = match.groupValues[3]

            if (!potentialUrlString.contains(urlRegex)) {
                "$start$resolveNamespaceString$potentialUrlString$end"
            } else {
                "$start$potentialUrlString$end"
            }
        }
    }

    private fun Document.asString(outputFormat: OutputFormat = SquitOutputFormat): String {
        return StringWriter()
            .also { XMLWriter(it, outputFormat).write(this) }
            .toString()
    }
}
