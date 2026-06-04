package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.entity.SquitOutputFormat
import de.smartsquare.squit.mediatype.Canonicalizer
import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import org.apache.xml.security.Init as ApacheInit
import org.apache.xml.security.c14n.Canonicalizer as ApacheCanonicalizer

/**
 * [Canonicalizer] for XML.
 */
class XmlCanonicalizer : Canonicalizer {

    private val canonicalizer: ApacheCanonicalizer

    init {
        ApacheInit.init()

        canonicalizer = ApacheCanonicalizer.getInstance(ApacheCanonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS)
    }

    private companion object {
        private const val RESOLVE_NAMESPACE_STRING = "http://"

        private val xmlNamespaceRegex = Regex("(xmlns:\\w+=['\"])(.*?)(['\"])")
        private val urlRegex = Regex("^https?://")
    }

    override fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String =
        if (mediaTypeConfig.xmlCanonicalize) {
            val content = if (mediaTypeConfig.xmlResolveInvalidNamespaces) {
                resolveInvalidNamespaces(input)
            } else {
                input
            }

            val canonicalized = UncopiedByteArrayOutputStream()
                .also { canonicalizer.canonicalize(content.toByteArray(), it, false) }

            SAXReader().read(canonicalized.asInputStream()).asString()
        } else {
            input
        }

    private fun resolveInvalidNamespaces(content: String): String = content.replace(xmlNamespaceRegex) { match ->
        val start = match.groupValues[1]
        val potentialUrlString = match.groupValues[2]
        val end = match.groupValues[3]

        if (!potentialUrlString.contains(urlRegex)) {
            "$start$RESOLVE_NAMESPACE_STRING$potentialUrlString$end"
        } else {
            "$start$potentialUrlString$end"
        }
    }

    private class UncopiedByteArrayOutputStream : ByteArrayOutputStream() {
        fun asInputStream() = ByteArrayInputStream(buf, 0, count)
    }

    private fun Document.asString(outputFormat: OutputFormat = SquitOutputFormat): String = StringWriter()
        .also { XMLWriter(it, outputFormat).write(this) }
        .toString()
}
