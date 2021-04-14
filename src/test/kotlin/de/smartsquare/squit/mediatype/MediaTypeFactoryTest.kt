package de.smartsquare.squit.mediatype

import de.smartsquare.squit.mediatype.generic.GenericBodyProcessor
import de.smartsquare.squit.mediatype.generic.GenericCanonicalizer
import de.smartsquare.squit.mediatype.generic.GenericDiffer
import de.smartsquare.squit.mediatype.json.JsonBodyProcessor
import de.smartsquare.squit.mediatype.json.JsonCanonicalizer
import de.smartsquare.squit.mediatype.json.JsonDiffer
import de.smartsquare.squit.mediatype.xml.XmlBodyProcessor
import de.smartsquare.squit.mediatype.xml.XmlCanonicalizer
import de.smartsquare.squit.mediatype.xml.XmlDiffer
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MediaTypeFactoryTest {

    @Test
    fun `xml mediaType`() {
        val mediaType = "application/xml".toMediaType()

        MediaTypeFactory.request(mediaType) shouldBeEqualTo "request.xml"
        MediaTypeFactory.sourceResponse(mediaType) shouldBeEqualTo "response.xml"
        MediaTypeFactory.expectedResponse(mediaType) shouldBeEqualTo "expected_response.xml"
        MediaTypeFactory.actualResponse(mediaType) shouldBeEqualTo "actual_response.xml"
        MediaTypeFactory.processor(mediaType) shouldBeInstanceOf XmlBodyProcessor::class.java

        val differ = MediaTypeFactory.differ(mediaType, MediaTypeConfig())
        val canonicalizer = MediaTypeFactory.canonicalizer(mediaType)

        differ shouldBeInstanceOf XmlDiffer::class.java
        canonicalizer shouldBeInstanceOf XmlCanonicalizer::class.java
    }

    @Test
    fun `json mediaType`() {
        val mediaType = "application/json".toMediaType()

        MediaTypeFactory.request(mediaType) shouldBeEqualTo "request.json"
        MediaTypeFactory.sourceResponse(mediaType) shouldBeEqualTo "response.json"
        MediaTypeFactory.expectedResponse(mediaType) shouldBeEqualTo "expected_response.json"
        MediaTypeFactory.actualResponse(mediaType) shouldBeEqualTo "actual_response.json"
        MediaTypeFactory.processor(mediaType) shouldBeInstanceOf JsonBodyProcessor::class.java

        val differ = MediaTypeFactory.differ(mediaType, MediaTypeConfig())
        val canonicalizer = MediaTypeFactory.canonicalizer(mediaType)

        differ shouldBeInstanceOf JsonDiffer::class.java
        canonicalizer shouldBeInstanceOf JsonCanonicalizer::class.java
    }

    @Test
    fun `different mediaType`() {
        val mediaType = "plain/text".toMediaType()

        MediaTypeFactory.request(mediaType) shouldBeEqualTo "request.txt"
        MediaTypeFactory.sourceResponse(mediaType) shouldBeEqualTo "response.txt"
        MediaTypeFactory.expectedResponse(mediaType) shouldBeEqualTo "expected_response.txt"
        MediaTypeFactory.actualResponse(mediaType) shouldBeEqualTo "actual_response.txt"
        MediaTypeFactory.processor(mediaType) shouldBeInstanceOf GenericBodyProcessor::class.java

        val differ = MediaTypeFactory.differ(mediaType, MediaTypeConfig())
        val canonicalizer = MediaTypeFactory.canonicalizer(mediaType)

        differ shouldBeInstanceOf GenericDiffer::class.java
        canonicalizer shouldBeInstanceOf GenericCanonicalizer::class.java
    }
}
