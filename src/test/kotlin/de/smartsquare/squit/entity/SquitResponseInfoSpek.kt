package de.smartsquare.squit.entity

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object SquitResponseInfoSpek : Spek({

    given("a response info with only a response code") {
        val subject = SquitResponseInfo(200)

        on("converting to json and back") {
            val subjectAsJson = subject.toJson()
            val subjectFromJson = SquitResponseInfo.fromJson(subjectAsJson)

            it("should equal the expected result") {
                subjectFromJson shouldBeEqualTo subject
            }
        }

        on("checking the default") {
            it("should not be a default") {
                false shouldBeEqualTo subject.isDefault
            }
        }
    }

    given("a response info with the default response code") {
        val subject = SquitResponseInfo()

        on("converting to json and back") {
            val subjectAsJson = subject.toJson()
            val subjectFromJson = SquitResponseInfo.fromJson(subjectAsJson)

            it("should equal the expected result") {
                subjectFromJson shouldBeEqualTo subject
            }
        }

        on("checking the default") {
            it("should be a default") {
                true shouldBeEqualTo subject.isDefault
            }
        }
    }
})
