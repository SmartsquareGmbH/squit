package de.smartsquare.squit.mediatype

import com.typesafe.config.Config
import java.nio.file.Path

/**
 * Interface for processing request bodies before and after sending/receiving.
 */
interface BodyProcessor {

    /**
     * Processes the request and response at the given [requestPath] and [responsePath] with the given [config] and
     * writes the results to the [resultRequestPath] and [resultResponsePath].
     */
    fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    )

    /**
     * Processes the request and response at the given [actualResponsePath] and [expectedResponsePath] with the given
     * [config] and writes the resulting actual response to the [resultActualResponseFilePath].
     */
    fun postProcess(
        actualResponsePath: Path,
        expectedResponsePath: Path,
        resultActualResponseFilePath: Path,
        config: Config
    )
}
