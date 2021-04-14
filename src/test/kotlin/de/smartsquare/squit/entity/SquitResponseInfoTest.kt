package de.smartsquare.squit.entity

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SquitResponseInfoTest {

    @Test
    fun `response info with only a response code`() {
        val subject = SquitResponseInfo(200)

        subject.isDefault shouldBeEqualTo false

        val subjectAsJson = subject.toJson()
        val subjectFromJson = SquitResponseInfo.fromJson(subjectAsJson)

        subjectFromJson shouldBeEqualTo subject
    }

    @Test
    fun `response info with the default response code`() {
        val subject = SquitResponseInfo()

        true shouldBeEqualTo subject.isDefault

        val subjectAsJson = subject.toJson()
        val subjectFromJson = SquitResponseInfo.fromJson(subjectAsJson)

        subjectFromJson shouldBeEqualTo subject
    }
}
