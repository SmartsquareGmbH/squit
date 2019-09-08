package de.smartsquare.squit.mediatype

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.mediatype.generic.GenericBodyProcessor
import de.smartsquare.squit.mediatype.generic.GenericCanonicalizer
import de.smartsquare.squit.mediatype.generic.GenericDiffer
import de.smartsquare.squit.mediatype.json.JsonBodyProcessor
import de.smartsquare.squit.mediatype.json.JsonCanonicalizer
import de.smartsquare.squit.mediatype.json.JsonDiffer
import de.smartsquare.squit.mediatype.xml.XmlBodyProcessor
import de.smartsquare.squit.mediatype.xml.XmlDiffer
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Ruben Gees
 */
object MediaTypeFactorySpek : Spek({

    given("a xml mediaType") {
        val mediaType = "application/xml".toMediaTypeOrNull() ?: throw NullPointerException()

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
                MediaTypeFactory.processor(mediaType) shouldBeInstanceOf XmlBodyProcessor::class.java
            }
        }

        on("getting the differ") {
            it("should return the xml differ") {
                val differ = MediaTypeFactory.differ(mediaType, SquitExtension(ProjectBuilder.builder().build()))

                differ shouldBeInstanceOf XmlDiffer::class.java
            }
        }

        on("getting the canonicalizer") {
            it("should return the generic canonicalizer") {
                val differ = MediaTypeFactory.canonicalizer(mediaType)

                differ shouldBeInstanceOf GenericCanonicalizer::class.java
            }
        }
    }

    given("a json mediaType") {
        val mediaType = "application/json".toMediaTypeOrNull() ?: throw NullPointerException()

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
            it("should return the json processor") {
                MediaTypeFactory.processor(mediaType) shouldBeInstanceOf JsonBodyProcessor::class.java
            }
        }

        on("getting the differ") {
            it("should return the generic differ") {
                val differ = MediaTypeFactory.differ(mediaType, SquitExtension(ProjectBuilder.builder().build()))

                differ shouldBeInstanceOf JsonDiffer::class.java
            }
        }

        on("getting the canonicalizer") {
            it("should return the generic canonicalizer") {
                val differ = MediaTypeFactory.canonicalizer(mediaType)

                differ shouldBeInstanceOf JsonCanonicalizer::class.java
            }
        }
    }

    given("a different mediaType") {
        val mediaType = "plain/text".toMediaTypeOrNull() ?: throw NullPointerException()

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
            it("should return the generic processor") {
                MediaTypeFactory.processor(mediaType) shouldBeInstanceOf GenericBodyProcessor::class.java
            }
        }

        on("getting the differ") {
            it("should return the generic differ") {
                val differ = MediaTypeFactory.differ(mediaType, SquitExtension(ProjectBuilder.builder().build()))

                differ shouldBeInstanceOf GenericDiffer::class.java
            }
        }

        on("getting the canonicalizer") {
            it("should return the generic canonicalizer") {
                val differ = MediaTypeFactory.canonicalizer(mediaType)

                differ shouldBeInstanceOf GenericCanonicalizer::class.java
            }
        }
    }
})
