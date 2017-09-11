package de.smartsquare.timrunner.util

import java.nio.file.Path

object Utils {

    fun getTestIndex(testDirectoryPath: Path) = testDirectoryPath.fileName.toString().let {
        it.substring(0, it.indexOf("-")).toInt()
    }
}