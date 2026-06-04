package de.smartsquare.squit.mediatype

import java.nio.file.Path

/**
 * Interface for generating diffs between expected and actual responses.
 */
interface Differ {

    /**
     * Calculates and return a user-friendly difference between the given [expectedResponsePath] and
     * [actualResponsePath].
     */
    fun diff(expectedResponsePath: Path, actualResponsePath: Path): String
}
