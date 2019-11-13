package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.mediatype.Differ
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.io.ByteArrayInputStream

class XmlDiffer(private val extension: SquitExtension) : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        val differ = DiffBuilder.compare(Input.fromStream(ByteArrayInputStream(expectedResponse)))
            .withTest(Input.fromStream(ByteArrayInputStream(actualResponse)))
            .ignoreWhitespace()
            .apply { if (!extension.xml.strict) checkForSimilar() }
            .build()

        return differ.differences.joinToString("\n")
    }
}
