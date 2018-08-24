package de.smartsquare.squit.mediatype.generic

import com.github.difflib.DiffUtils
import de.smartsquare.squit.mediatype.Differ
import java.nio.charset.Charset

/**
 * @author Ruben Gees
 */
class GenericDiffer : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        val diff = DiffUtils.diff(expectedResponse.toString(Charset.defaultCharset()),
            actualResponse.toString(Charset.defaultCharset()))

        return diff.deltas.joinToString("\n") {
            "Expected '${it.original.lines.joinToString("\n")}' but was " +
                "'${it.revised.lines.joinToString("\n")}' at line ${it.original.position + 1}"
        }
    }
}
