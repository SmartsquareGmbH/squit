package de.smartsquare.squit.mediatype

import de.smartsquare.squit.mediatype.generic.GenericBodyProcessor
import de.smartsquare.squit.mediatype.xml.XmlBodyProcessor
import okhttp3.MediaType
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Ruben Gees
 */
object MediaTypeFactorySpek : Spek({

    given("a xml mediaType") {
        val mediaType = MediaType.parse("application/xml") ?: throw NullPointerException()

        on("getting the request") {
            it("should return a correct name") {
                MediaTypeFactory.request(mediaType) shouldEqual "request.xml"
            }
        }

        on("getting the source response") {
            it("should return the correct name") {
                MediaTypeFactory.sourceResponse(mediaType) shouldEqual "response.xml"
            }
        }

        on("getting the expected response") {
            it("should return the correct name") {
                MediaTypeFactory.expectedResponse(mediaType) shouldEqual "expected_response.xml"
            }
        }

        on("getting the actual response") {
            it("should return the correct name") {
                MediaTypeFactory.actualResponse(mediaType) shouldEqual "actual_response.xml"
            }
        }

        on("getting the processor") {
            it("should return the xml processor") {
                MediaTypeFactory.processor(mediaType) shouldBe XmlBodyProcessor
            }
        }
    }

    given("a json mediaType") {
        val mediaType = MediaType.parse("application/json") ?: throw NullPointerException()

        on("getting the request") {
            it("should return a correct name") {
                MediaTypeFactory.request(mediaType) shouldEqual "request.json"
            }
        }

        on("getting the source response") {
            it("should return the correct name") {
                MediaTypeFactory.sourceResponse(mediaType) shouldEqual "response.json"
            }
        }

        on("getting the expected response") {
            it("should return the correct name") {
                MediaTypeFactory.expectedResponse(mediaType) shouldEqual "expected_response.json"
            }
        }

        on("getting the actual response") {
            it("should return the correct name") {
                MediaTypeFactory.actualResponse(mediaType) shouldEqual "actual_response.json"
            }
        }

        on("getting the processor") {
            it("should return the default processor") {
                MediaTypeFactory.processor(mediaType) shouldBe GenericBodyProcessor
            }
        }
    }

    given("a different mediaType") {
        val mediaType = MediaType.parse("plain/text") ?: throw NullPointerException()

        on("getting the request") {
            it("should return a correct name") {
                MediaTypeFactory.request(mediaType) shouldEqual "request.txt"
            }
        }

        on("getting the source response") {
            it("should return the correct name") {
                MediaTypeFactory.sourceResponse(mediaType) shouldEqual "response.txt"
            }
        }

        on("getting the expected response") {
            it("should return the correct name") {
                MediaTypeFactory.expectedResponse(mediaType) shouldEqual "expected_response.txt"
            }
        }

        on("getting the actual response") {
            it("should return the correct name") {
                MediaTypeFactory.actualResponse(mediaType) shouldEqual "actual_response.txt"
            }
        }

        on("getting the processor") {
            it("should return the default processor") {
                MediaTypeFactory.processor(mediaType) shouldBe GenericBodyProcessor
            }
        }
    }
})
