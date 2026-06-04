package de.smartsquare.squit.mediatype.generic

import com.github.difflib.DiffUtils
import de.smartsquare.squit.mediatype.Differ
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generic [Differ] for all other media types.
 */
class GenericDiffer : Differ {

    override fun diff(expectedResponsePath: Path, actualResponsePath: Path): String {
        val diff = DiffUtils.diff(
            Files.readString(expectedResponsePath),
            Files.readString(actualResponsePath),
            null,
        )

        return diff.deltas.joinToString("\n") {
            "Expected '${it.source.lines.joinToString("\n")}' but was " +
                "'${it.target.lines.joinToString("\n")}' at line ${it.target.position + 1}"
        }
    }
}
