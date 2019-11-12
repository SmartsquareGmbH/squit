package de.smartsquare.squit.io

import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.gradle.api.GradleException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Object extending the [SAXReader] with utility methods.
 *
 * @author Ruben Gees
 */
object SAXReaderSupport {

    /**
     * Reads and returns a [org.dom4j.Document] at the given [path].
     *
     * This is a safe operation, as such the file is correctly closed.
     */
    fun read(path: Path): Document = try {
        Files.newBufferedReader(path).use {
            SAXReader().read(it)
        }
    } catch (error: IOException) {
        throw GradleException("Could not read xml file: $path", error)
    }
}
