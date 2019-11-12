package de.smartsquare.squit.io

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.gradle.api.GradleException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Object extending the [JsonParser] with utility methods.
 *
 * @author Ruben Gees
 */
object JsonParserSupport {

    /**
     * Reads the file at the given [path] and returns a [JsonElement].
     *
     * This is a safe operation, as such the file is correctly closed.
     */
    fun read(path: Path): JsonElement = try {
        val jsonElement = Files.newBufferedReader(path).use {
            JsonParser.parseReader(it)
        }

        jsonElement
    } catch (error: IOException) {
        throw GradleException("Could not read json file: $path", error)
    } catch (error: JsonParseException) {
        throw GradleException("Could not read json file: $path", error)
    }
}
