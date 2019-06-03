package de.smartsquare.squit.entity

import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Sascha Koch
 */
object SquitResponseInfoSpek : Spek({

    given("a response info with only a response code") {
        val subject = SquitResponseInfo("200")

        on("converting to json and back") {
            val subjectAsJson = subject.toJson()
            val subjectFromJson = SquitResponseInfo.fromJson(subjectAsJson)

            it("should equal the expected result") {
                subjectFromJson shouldEqual subject
            }
        }
    }
})
