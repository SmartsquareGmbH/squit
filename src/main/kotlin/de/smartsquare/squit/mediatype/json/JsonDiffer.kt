package de.smartsquare.squit.mediatype.json

import de.smartsquare.squit.mediatype.Differ
import net.javacrumbs.jsonunit.JsonAssert

/**
 * @author Ruben Gees
 */
class JsonDiffer : Differ {

    override fun diff(expectedResponse: ByteArray, actualResponse: ByteArray): String {
        return try {
            JsonAssert.assertJsonEquals(
                expectedResponse.toString(Charsets.UTF_8),
                actualResponse.toString(Charsets.UTF_8)
            )

            ""
        } catch (error: AssertionError) {
            requireNotNull(error.message)
        }
    }
}
