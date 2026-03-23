package de.smartsquare.squit.mediatype.generic

import com.github.difflib.DiffUtils
import de.smartsquare.squit.mediatype.Differ

/**
 * Generic [Differ] for all other media types.
 */
class GenericDiffer : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        val diff = DiffUtils.diff(
            expectedResponse.toString(Charsets.UTF_8),
            actualResponse.toString(Charsets.UTF_8),
            null,
        )

        return diff.deltas.joinToString("\n") {
            "Expected '${it.source.lines.joinToString("\n")}' but was " +
                "'${it.target.lines.joinToString("\n")}' at line ${it.target.position + 1}"
        }
    }
}
