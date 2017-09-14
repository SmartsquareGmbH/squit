package de.smartsquare.timrunner.util

import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldStartWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.properties.Delegates

class UtilExtensionsTest {

    private var temporaryPath by Delegates.notNull<Path>()

    @Before
    fun setup() {
        temporaryPath = Files.createTempFile("test-", ".tmp")
    }

    @Test
    fun cut() {
        val path = Paths.get("a").resolve("b")

        path.cut(Paths.get("a")) shouldEqual Paths.get("b")
    }

    @Test
    fun cutWithoutMatch() {
        val path = Paths.get("c").resolve("d")

        path.cut(Paths.get("a")) shouldEqual path
    }

    @Test
    fun safeLoad() {
        Files.write(temporaryPath, "a=b".toByteArray())

        Properties().safeLoad(temporaryPath) shouldContain ("a" to "b")
    }

    @Test
    fun safeStore() {
        Properties().apply { setProperty("a", "b") }.safeStore(temporaryPath)

        Files.readAllBytes(temporaryPath).toString(Charsets.UTF_8) shouldContain "a=b"
    }

    @Test
    fun safeStoreComment() {
        Properties().apply { setProperty("a", "b") }.safeStore(temporaryPath, "testComment")

        Files.readAllBytes(temporaryPath).toString(Charsets.UTF_8) shouldStartWith "#testComment"
    }

    @After
    fun tearDown() {
        Files.deleteIfExists(temporaryPath)
    }
}