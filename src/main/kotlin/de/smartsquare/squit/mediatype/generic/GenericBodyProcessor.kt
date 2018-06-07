package de.smartsquare.squit.mediatype.generic

import com.typesafe.config.Config
import de.smartsquare.squit.mediatype.BodyProcessor
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generic [BodyProcessor] implementation which simply copies over the request and responses.
 *
 * @author Ruben Gees
 */
object GenericBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        if (requestPath != null) Files.copy(requestPath, resultRequestPath)
        Files.copy(responsePath, resultResponsePath)
    }

    override fun postProcess(
        actualResponsePath: Path,
        expectedResponsePath: Path,
        resultActualResponseFilePath: Path,
        config: Config
    ) {
        Files.copy(actualResponsePath, resultActualResponseFilePath)
    }
}
