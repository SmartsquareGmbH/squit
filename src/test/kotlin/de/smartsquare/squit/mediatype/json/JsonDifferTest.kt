package de.smartsquare.squit.mediatype.json

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class JsonDifferTest {

    private val differ = JsonDiffer()

    @Test
    fun `equal json structures produce an empty diff`() {
        // language=json
        val expected = """{ "a": 1, "b": [1,2,3] }""".toByteArray()

        // language=json
        val actual = """{ "a": 1, "b": [1,2,3] }""".toByteArray()

        differ.diff(expected, actual) shouldBeEqualTo ""
    }

    @Test
    fun `differing json structures produce a non-empty diff`() {
        // language=json
        val expected = """{ "a": 1 }""".toByteArray()

        // language=json
        val actual = """{ "a": 2 }""".toByteArray()

        val result = differ.diff(expected, actual)

        result shouldContain """Different value found in node "a", expected: <1> but was: <2>."""
    }

    @Test
    fun `malformed actual json is reported as a diff`() {
        // language=json
        val expected = """{ "a": 1 }""".toByteArray()

        val actual = "this is not valid json".toByteArray()

        val result = differ.diff(expected, actual)

        result shouldContain  "Can not parse fullJson value: 'this is not valid json'"
    }

    @Test
    fun `malformed expected json is reported as a diff`() {
        val expected = "invalid".toByteArray()

        // language=json
        val actual = """{ "a": 1 }""".toByteArray()

        val result = differ.diff(expected, actual)

        result shouldContain """Different value found in node "", expected: <"invalid"> but was: <{"a":1}>"""
    }
}
