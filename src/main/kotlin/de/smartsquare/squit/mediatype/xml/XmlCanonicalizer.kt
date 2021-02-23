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

    override fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String {
        return if (mediaTypeConfig.xmlCanonicalize) {
            val outputStream = ByteArrayOutputStream()

            val content = if (mediaTypeConfig.resolveInvalidNamespaces)
                resolveInvalidNamespaces(input, mediaTypeConfig.resolveNamespaceString)
            else
                input

            canonicalizer.canonicalize(content.toByteArray(), outputStream, false)

            SAXReader().read(outputStream.toByteArray().inputStream()).asString()
        } else {
            input
        }
    }

    private fun resolveInvalidNamespaces(content: String, resolveNamespaceString: String): String {
        var contentReplaced = content
        val regexNamespaces = Regex("xmlns([^\\s!>]+)")
        val regexNamespacesMatches = regexNamespaces.findAll(content)
        val urlRegex = Regex("[\"'][^\\s]+[\"']")
        for (match in regexNamespacesMatches) {
            val potentialUrl = urlRegex.find(match.value)
            if (potentialUrl != null) {
                val potentialUrlString = potentialUrl.value.replace("\"", "").replace("'", "")
                if (!potentialUrlString.startsWith("http://") && !potentialUrlString.startsWith("https://")) {
                    val ns = match.value.substringAfter("xmlns", "").substringBefore("=", "")
                    val replacement = "xmlns$ns=\"$resolveNamespaceString$potentialUrlString\""
                    contentReplaced = contentReplaced.replaceFirst(match.value, replacement)
                }
            }
        }
        return contentReplaced
    }

    private fun Document.asString(outputFormat: OutputFormat = SquitOutputFormat): String {
        return StringWriter()
            .also { XMLWriter(it, outputFormat).write(this) }
            .toString()
    }
}
