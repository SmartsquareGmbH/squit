package de.smartsquare.squit.entity

import java.nio.file.Paths
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SquitResultTest {

    private val mediaType = "application/xml".toMediaType()

    @Test
    fun `cutting the first path element of result with full path`() {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get("a"), Paths.get("b"), Paths.get("c"), Paths.get("x"),
        )

        val result = subject.cutFirstPathElement()

        result.fullPath shouldBeEqualTo Paths.get("b", "c")
    }

    @Test
    fun `cutting the first path element of a result without context path`() {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get("b"), Paths.get("c"), Paths.get("x"),
        )

        val result = subject.cutFirstPathElement()

        result.fullPath shouldBeEqualTo Paths.get("c")
    }

    @Test
    fun `cutting the first path element of a result with only testDirectoryPath`() {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get(""), Paths.get("c"), Paths.get("x"),
        )

        val result = subject.cutFirstPathElement()

        result.fullPath shouldBeEqualTo Paths.get("")
    }

    @Test
    fun `cutting the first path element of a result with empty path`() {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get(""), Paths.get(""), Paths.get("x"),
        )

        val result = subject.cutFirstPathElement()

        result.fullPath shouldBeEqualTo Paths.get("")
    }
}
