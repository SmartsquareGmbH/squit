package de.smartsquare.squit.util

import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Paths

object UtilExtensionsSpek : Spek({

    given("two paths") {
        val first = Paths.get("a/b/c/d/e")
        val second = Paths.get("a/b/c/d/g/x")

        on("invoking the cut function") {
            val result = first.cut(second)

            it("should return a correctly mutated path") {
                result shouldEqual Paths.get("e")
            }
        }
    }

    given("two empty paths") {
        val path = Paths.get("")

        on("invoking the cut function") {
            val result = path.cut(path)

            it("should not crash and return an empty path") {
                result shouldEqual Paths.get("")
            }
        }
    }
})
