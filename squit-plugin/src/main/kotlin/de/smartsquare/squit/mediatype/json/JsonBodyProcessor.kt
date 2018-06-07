package de.smartsquare.squit.mediatype.json

import com.google.gson.JsonParser
import com.typesafe.config.Config
import de.smartsquare.squit.mediatype.BodyProcessor
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.write
import java.nio.file.Path

/**
 * @author Ruben Gees
 */
object JsonBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        val request = requestPath?.let { JsonParser().read(it) }
        val response = JsonParser().read(responsePath)

        request?.write(resultRequestPath)
        response.write(resultResponsePath)
    }

    override fun postProcess(
        actualResponsePath: Path,
        expectedResponsePath: Path,
        resultActualResponseFilePath: Path,
        config: Config
    ) {
        val actualResponse = JsonParser().read(actualResponsePath)

        actualResponse.write(resultActualResponseFilePath)
    }
}
