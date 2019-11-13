package de.smartsquare.squit.mediatype

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.mediatype.generic.GenericBodyProcessor
import de.smartsquare.squit.mediatype.generic.GenericCanonicalizer
import de.smartsquare.squit.mediatype.generic.GenericDiffer
import de.smartsquare.squit.mediatype.json.JsonBodyProcessor
import de.smartsquare.squit.mediatype.json.JsonCanonicalizer
import de.smartsquare.squit.mediatype.json.JsonDiffer
import de.smartsquare.squit.mediatype.xml.XmlBodyProcessor
import de.smartsquare.squit.mediatype.xml.XmlCanonicalizer
import de.smartsquare.squit.mediatype.xml.XmlDiffer
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType

/**
 * Object for retrieving media type specific values.
 */
object MediaTypeFactory {

    val xmlMediaType = "text/xml".toMediaType()
    val applicationXmlMediaType = "application/xml".toMediaType()
    val soapMediaType = "application/soap+xml".toMediaType()
    val jsonMediaType = "application/json".toMediaType()

    /**
     * Returns the request name based on the given [mediaType].
     */
    fun request(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> "request.xml"
        jsonMediaType -> "request.json"
        else -> "request.txt"
    }

    /**
     * Returns the source response name based on the given [mediaType].
     */
    fun sourceResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> "response.xml"
        jsonMediaType -> "response.json"
        else -> "response.txt"
    }

    /**
     * Returns the expected response name based on the given [mediaType].
     */
    fun expectedResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> "expected_response.xml"
        jsonMediaType -> "expected_response.json"
        else -> "expected_response.txt"
    }

    /**
     * Returns the actual response name based on the given [mediaType].
     */
    fun actualResponse(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> "actual_response.xml"
        jsonMediaType -> "actual_response.json"
        else -> "actual_response.txt"
    }

    /**
     * Returns the [BodyProcessor] to use based on the given [mediaType].
     */
    fun processor(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> XmlBodyProcessor()
        jsonMediaType -> JsonBodyProcessor()
        else -> GenericBodyProcessor()
    }

    /**
     * Returns the [Differ] to use based on the given [mediaType].
     */
    fun differ(mediaType: MediaType, extension: SquitExtension) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> XmlDiffer(extension)
        jsonMediaType -> JsonDiffer()
        else -> GenericDiffer()
    }

    /**
     * Returns the [Canonicalizer] to use based on the given [mediaType].
     */
    fun canonicalizer(mediaType: MediaType) = when (mediaType) {
        xmlMediaType, applicationXmlMediaType, soapMediaType -> XmlCanonicalizer()
        jsonMediaType -> JsonCanonicalizer()
        else -> GenericCanonicalizer()
    }
}
