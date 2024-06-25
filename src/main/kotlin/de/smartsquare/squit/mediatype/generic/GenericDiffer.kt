package de.smartsquare.squit.mediatype.generic

import com.github.difflib.DiffUtils
import de.smartsquare.squit.mediatype.Differ
import java.nio.charset.Charset

/**
 * Generic [Differ] for all other media types.
 */
class GenericDiffer : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        val diff = DiffUtils.diff(
            expectedResponse.toString(Charset.defaultCharset()),
            actualResponse.toString(Charset.defaultCharset()),
            null,
        )

        return diff.deltas.joinToString("\n") {
            "Expected '${it.source.lines.joinToString("\n")}' but was " +
                "'${it.target.lines.joinToString("\n")}' at line ${it.target.position + 1}"
        }
    }
}
