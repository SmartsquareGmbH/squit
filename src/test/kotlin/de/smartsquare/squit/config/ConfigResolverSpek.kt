package de.smartsquare.squit.config

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.TestUtils
import io.mockk.every
import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldMatchAtLeastOneOf
import org.amshove.kluent.shouldThrow
import org.gradle.api.GradleException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ConfigResolverSpek : Spek({

    val testProject = TestUtils.getResourcePath("test-project").resolve("src/test")

    val configWalker = mockk<ConfigWalker>()
    val configResolver = ConfigResolver(configWalker, testProject, mockk(relaxed = true))

    given("a test project") {
        on("resolving leaf directories with configs") {
            every { configWalker.walk(any()) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com")
            )

            val leafDirectories = configResolver.resolveWithLeafDirectories(emptyList(), false)

            it("should return a correct list") {
                leafDirectories.size shouldEqual 4
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call1") }
                leafDirectories.shouldMatchAtLeastOneOf { it.second.endpoint == "https://example.com".toHttpUrl() }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call2") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call3") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call4") }
            }
        }

        on("filtering with tags") {
            every { configWalker.walk(match { it.endsWith("call1") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "tags" to listOf("call1"))
            )
            every { configWalker.walk(match { it.endsWith("call2") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "tags" to listOf("call2"))
            )

            every { configWalker.walk(match { it.endsWith("call3") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "tags" to listOf("call3"))
            )

            val leafDirectories = configResolver.resolveWithLeafDirectories(listOf("call1", "call3"), false)

            it("should return a correct list") {
                leafDirectories.size shouldEqual 2
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call1") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call3") }
            }
        }

        on("filtering with exclude") {
            every { configWalker.walk(match { it.endsWith("call1") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "exclude" to true)
            )
            every { configWalker.walk(match { it.endsWith("call2") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "exclude" to false)
            )

            every { configWalker.walk(match { it.endsWith("call3") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com")
            )

            val leafDirectories = configResolver.resolveWithLeafDirectories(emptyList(), false)

            it("should return a correct list") {
                leafDirectories.size shouldEqual 3
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call2") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call3") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call4") }
            }
        }

        on("filtering with exclude but setting the unexclude flag") {
            every { configWalker.walk(match { it.endsWith("call1") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "exclude" to true)
            )
            every { configWalker.walk(match { it.endsWith("call2") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com", "exclude" to false)
            )

            every { configWalker.walk(match { it.endsWith("call3") }) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "https://example.com")
            )

            val leafDirectories = configResolver.resolveWithLeafDirectories(emptyList(), true)

            it("should return a correct list") {
                leafDirectories.size shouldEqual 4
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call1") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call2") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call3") }
                leafDirectories.shouldMatchAtLeastOneOf { it.first.toString().endsWith("call4") }
            }
        }

        on("resolving with invalid config") {
            every { configWalker.walk(any()) } returns ConfigFactory.parseMap(
                mapOf("endpoint" to "invalid")
            )

            val call = { configResolver.resolveWithLeafDirectories(emptyList(), false) }

            call shouldThrow GradleException::class
        }
    }
})
