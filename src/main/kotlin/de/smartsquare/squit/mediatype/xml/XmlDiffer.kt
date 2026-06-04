package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.mediatype.Differ
import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.nio.file.Path

/**
 * [Differ] for XML.
 */
class XmlDiffer(private val mediaTypeConfig: MediaTypeConfig) : Differ {

    override fun diff(expectedResponsePath: Path, actualResponsePath: Path): String {
        val differ = DiffBuilder.compare(Input.fromPath(expectedResponsePath))
            .withTest(Input.fromPath(actualResponsePath))
            .ignoreWhitespace()
            .apply { if (!mediaTypeConfig.xmlStrict) checkForSimilar() }
            .build()

        return differ.differences.joinToString("\n")
    }
}
