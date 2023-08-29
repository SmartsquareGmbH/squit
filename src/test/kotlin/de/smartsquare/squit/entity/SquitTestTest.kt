package de.smartsquare.squit.entity

import com.typesafe.config.ConfigFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Paths
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SquitTestTest {

    @Test
    fun `merging two SquitTest objects`() {
        val test1 = SquitTest(
            Paths.get("/test"),
            ConfigFactory.parseMap(mapOf("a" to "b")),
            Paths.get("/request"),
            Paths.get("/response"),
            mapOf("test1_pre.sql" to listOf(Paths.get("/pre/test1_pre.sql"))),
            mapOf("test1_post.sql" to listOf(Paths.get("/post/test1_post.sql"))),
            listOf(Paths.get("/test1/description.md")),
        )

        val test2 = SquitTest(
            Paths.get("/test"),
            ConfigFactory.parseMap(mapOf("a" to "b")),
            Paths.get("/request"),
            Paths.get("/response"),
            mapOf(
                "test2_pre.sql" to listOf(Paths.get("/pre/test2_pre.sql")),
                "test1_pre.sql" to listOf(Paths.get("/additional/test1_pre.sql")),
            ),
            mapOf("test2_post.sql" to listOf(Paths.get("/post/test2_post.sql"))),
            listOf(Paths.get("/test2/description.md")),
        )

        val expected = SquitTest(
            Paths.get("/test"),
            ConfigFactory.parseMap(mapOf("a" to "b")),
            Paths.get("/request"),
            Paths.get("/response"),
            mapOf(
                "test1_pre.sql" to listOf(Paths.get("/additional/test1_pre.sql"), Paths.get("/pre/test1_pre.sql")),
                "test2_pre.sql" to listOf(Paths.get("/pre/test2_pre.sql")),
            ),
            mapOf(
                "test1_post.sql" to listOf(Paths.get("/post/test1_post.sql")),
                "test2_post.sql" to listOf(Paths.get("/post/test2_post.sql")),
            ),
            listOf(Paths.get("/test2/description.md"), Paths.get("/test1/description.md")),
        )

        test1.merge(test2) shouldBeEqualTo expected
    }

    @Test
    fun `serializing a SquitTest object`() {
        val test = SquitTest(
            Paths.get("/test.xml"),
            ConfigFactory.parseMap(mapOf("a" to "b")),
            Paths.get("/request.xml"),
            Paths.get("/response.xml"),
            mapOf("test_pre.sql" to listOf(Paths.get("/pre/test_pre.sql"))),
            mapOf("test_post.sql" to listOf(Paths.get("/post/test_pre.sql"))),
            listOf(Paths.get("/description/description.md")),
        )

        val serialized = ByteArrayOutputStream().let { byteOut ->
            byteOut.use {
                ObjectOutputStream(byteOut).use { objectOut ->
                    objectOut.writeObject(test)
                }
            }

            byteOut.toByteArray()
        }

        val deserialized = ByteArrayInputStream(serialized).use { byteIn ->
            ObjectInputStream(byteIn).use { objectIn ->
                objectIn.readObject()
            }
        }

        deserialized shouldBeInstanceOf SquitTest::class.java
        deserialized shouldBeEqualTo test
    }
}
