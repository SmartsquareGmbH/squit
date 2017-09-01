package de.smartsquare.timrunner.util

import java.io.File
import java.io.FileFilter

/**
 * [FileFilter] for filtering only directories.
 *
 * @author Ruben Gees
 */
class DirectoryFilter : FileFilter {
    override fun accept(file: File) = file.isDirectory
}
