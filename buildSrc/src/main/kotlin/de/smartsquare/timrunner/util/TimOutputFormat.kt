package de.smartsquare.timrunner.util

import org.dom4j.io.OutputFormat

class TimOutputFormat : OutputFormat() {

    init {
        setIndentSize(4)

        isNewLineAfterDeclaration = false
//        isSuppressDeclaration = true
        isNewlines = true
        isTrimText = true
        isPadText = true
    }
}
