package de.smartsquare.squit.mediatype

import de.smartsquare.squit.mediatype.generic.GenericBodyProcessor
import de.smartsquare.squit.mediatype.generic.GenericDiffer
import de.smartsquare.squit.mediatype.xml.XmlBodyProcessor
import de.smartsquare.squit.mediatype.xml.XmlDiffer
import okhttp3.MediaType

/**
 * Object for retrieving media type specific values.
 *
 * @author Ruben Gees
 */
object MediaTypeFactory {

    private val xmlMediaType = MediaType.parse("application/xml")
    private val jsonMediaType = MediaType.parse("application/json")

    /**
     * Returns the request name based on the given [mediaType].
     */
    fun request(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> "request.xml"
        jsonMediaType -> "request.json"
        else -> "request.txt"
    }

    /**
     * Returns the source response name based on the given [mediaType].
     */
    fun sourceResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> "response.xml"
        jsonMediaType -> "response.json"
        else -> "response.txt"
    }

    /**
     * Returns the expected response name based on the given [mediaType].
     */
    fun expectedResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> "expected_response.xml"
        jsonMediaType -> "expected_response.json"
        else -> "expected_response.txt"
    }

    /**
     * Returns the actual response name based on the given [mediaType].
     */
    fun actualResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> "actual_response.xml"
        jsonMediaType -> "actual_response.json"
        else -> "actual_response.txt"
    }

    /**
     * Returns the [BodyProcessor] to use based on the given [mediaType].
     */
    fun processor(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> XmlBodyProcessor
        else -> GenericBodyProcessor
    }

    /**
     * Returns the [Differ] to use based on the given [mediaType].
     */
    fun differ(mediaType: MediaType) = when (mediaType) {
        xmlMediaType -> XmlDiffer
        else -> GenericDiffer
    }
}
