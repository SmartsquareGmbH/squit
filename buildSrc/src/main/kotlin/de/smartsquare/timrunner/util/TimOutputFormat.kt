package de.smartsquare.timrunner.util

import org.dom4j.io.OutputFormat

class TimOutputFormat : OutputFormat() {

    init {
        setIndentSize(2)

        isNewLineAfterDeclaration = false
        isNewlines = true
        isTrimText = true
    }
}
