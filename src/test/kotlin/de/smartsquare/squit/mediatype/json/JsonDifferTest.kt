package de.smartsquare.squit.mediatype.json

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class JsonDifferTest {

    private val differ = JsonDiffer()

    @TempDir
    private lateinit var tempDir: Path

    private fun jsonPath(name: String, content: String): Path =
        tempDir.resolve(name).also { Files.writeString(it, content) }

    @Test
    fun `equal json structures produce an empty diff`() {
        // language=json
        val expected = """{ "a": 1, "b": [1,2,3] }"""

        // language=json
        val actual = """{ "a": 1, "b": [1,2,3] }"""

        differ.diff(jsonPath("expected.json", expected), jsonPath("actual.json", actual)) shouldBeEqualTo ""
    }

    @Test
    fun `differing json structures produce a non-empty diff`() {
        // language=json
        val expected = """{ "a": 1 }"""

        // language=json
        val actual = """{ "a": 2 }"""

        val result = differ.diff(jsonPath("expected.json", expected), jsonPath("actual.json", actual))

        result shouldContain """Different value found in node "a", expected: <1> but was: <2>."""
    }

    @Test
    fun `malformed actual json is reported as a diff`() {
        // language=json
        val expected = """{ "a": 1 }"""

        val actual = "this is not valid json"

        val result = differ.diff(jsonPath("expected.json", expected), jsonPath("actual.json", actual))

        result shouldContain "Can not parse fullJson value: 'this is not valid json'"
    }

    @Test
    fun `malformed expected json is reported as a diff`() {
        val expected = "invalid"

        // language=json
        val actual = """{ "a": 1 }"""

        val result = differ.diff(jsonPath("expected.json", expected), jsonPath("actual.json", actual))

        result shouldContain """Different value found in node "", expected: <"invalid"> but was: <{"a":1}>"""
    }
}
