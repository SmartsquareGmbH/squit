package de.smartsquare.squit.util

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class UtilExtensionsTest {

    @Test
    fun `cutting two paths`() {
        val first = Paths.get("a/b/c/d/e")
        val second = Paths.get("a/b/c/d/g/x")

        first.cut(second) shouldBeEqualTo Paths.get("e")
    }

    @Test
    fun `cutting two empty paths`() {
        val path = Paths.get("")

        path.cut(path) shouldBeEqualTo Paths.get("")
    }
}
