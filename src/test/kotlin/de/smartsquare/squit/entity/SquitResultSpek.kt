package de.smartsquare.squit.entity

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Paths

object SquitResultSpek : Spek({

    val mediaType = "application/xml".toMediaTypeOrNull() ?: throw NullPointerException()

    given("a result with a full path") {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get("a"), Paths.get("b"), Paths.get("c"), Paths.get("x")
        )

        on("cutting the first path element") {
            val result = subject.cutFirstPathElement()

            it("should be modified correctly") {
                result.fullPath shouldEqual Paths.get("b", "c")
            }
        }
    }

    given("a result without a context path") {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get("b"), Paths.get("c"), Paths.get("x")
        )

        on("cutting the first path element") {
            val result = subject.cutFirstPathElement()

            it("should be modified correctly") {
                result.fullPath shouldEqual Paths.get("c")
            }
        }
    }

    given("a result with only a testDirectoryPath") {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get(""), Paths.get("c"), Paths.get("x")
        )

        on("cutting the first path element") {
            val result = subject.cutFirstPathElement()

            it("should be modified correctly") {
                result.fullPath shouldEqual Paths.get("")
            }
        }
    }

    given("a result with an empty path") {
        val subject = SquitResult(
            0, "", SquitResponseInfo(), false, mediaType, "",
            Paths.get(""), Paths.get(""), Paths.get(""), Paths.get("x")
        )

        on("cutting the first path element") {
            val result = subject.cutFirstPathElement()

            it("should not be modified") {
                result.fullPath shouldEqual Paths.get("")
            }
        }
    }
})
