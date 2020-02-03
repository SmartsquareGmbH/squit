package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.mediatype.Differ
import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.io.ByteArrayInputStream

/**
 * [Differ] for xml.
 */
class XmlDiffer(private val mediaTypeConfig: MediaTypeConfig) : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        val differ = DiffBuilder.compare(Input.fromStream(ByteArrayInputStream(expectedResponse)))
            .withTest(Input.fromStream(ByteArrayInputStream(actualResponse)))
            .ignoreWhitespace()
            .apply { if (!mediaTypeConfig.xmlStrict) checkForSimilar() }
            .build()

        return differ.differences.joinToString("\n")
    }
}
