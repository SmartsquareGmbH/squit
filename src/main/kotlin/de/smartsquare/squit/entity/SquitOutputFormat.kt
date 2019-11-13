package de.smartsquare.squit.entity

import org.dom4j.io.OutputFormat

/**
 * Custom output format for dom4j.
 */
object SquitOutputFormat : OutputFormat() {

    init {
        setIndentSize(2)

        isNewlines = true
        isTrimText = true
    }
}
