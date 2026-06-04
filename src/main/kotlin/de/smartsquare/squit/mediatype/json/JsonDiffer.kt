package de.smartsquare.squit.mediatype.json

import de.smartsquare.squit.mediatype.Differ
import net.javacrumbs.jsonunit.JsonAssert
import java.nio.file.Files
import java.nio.file.Path

/**
 * [Differ] for JSON.
 */
class JsonDiffer : Differ {

    override fun diff(expectedResponsePath: Path, actualResponsePath: Path): String = try {
        JsonAssert.assertJsonEquals(
            Files.readString(expectedResponsePath),
            Files.readString(actualResponsePath),
        )

        ""
    } catch (error: AssertionError) {
        error.message ?: error.toString()
    } catch (error: RuntimeException) {
        "JSON comparison failed: ${error.message ?: error.toString()}"
    }
}
