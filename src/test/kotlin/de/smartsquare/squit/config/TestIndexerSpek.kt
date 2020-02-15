package de.smartsquare.squit.config

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.TestUtils
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldHaveKey
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Paths

object TestIndexerSpek : Spek({

    val testProjectRoot = TestUtils.getResourcePath("test-project")

    val testIndexer = TestIndexer(
        ConfigFactory.parseMap(
            mapOf(
                "rootDir" to "$testProjectRoot",
                "endpointPlaceholder" to "https://example.com"
            )
        )
    )

    given("a test project") {
        val testProject = testProjectRoot.resolve("src/squit")

        on("indexing") {
            val index = testIndexer.index(testProject) { true }

            it("should return a correct index") {
                index.size shouldBeEqualTo 4
                index[0].path.toString() shouldEndWith "call1"
                index[0].config.hasPath("headers").shouldBeTrue()
                index[0].config.getStringList("tags").size shouldBeEqualTo 3
                index[0].config.getStringList("tags") shouldContain "unique"
                index[0].config.getStringList("tags") shouldContain "call1"
                index[0].config.getStringList("tags") shouldContain "project"
                index[0].preSqlScripts.size shouldBeEqualTo 1
                index[0].preSqlScripts shouldHaveKey "test"
                index[0].preSqlScripts.getValue("test").size shouldBeEqualTo 2
                index[0].preSqlScripts.getValue("test")[0].toString() shouldEndWith "test_pre_once.sql"
                index[0].preSqlScripts.getValue("test")[1].toString() shouldEndWith "test_pre.sql"
                index[0].postSqlScripts.getValue("test").size shouldBeEqualTo 1
                index[0].postSqlScripts.getValue("test")[0].toString() shouldEndWith "test_post.sql"
                index[0].descriptions.size shouldBeEqualTo 2
                index[0].descriptions[0].toString() shouldEndWith Paths.get("project/description.md").toString()
                index[0].descriptions[1].toString() shouldEndWith Paths.get("project/call1/description.md").toString()
                index[1].preSqlScripts.size shouldBeEqualTo 1
                index[1].preSqlScripts.getValue("test").size shouldBeEqualTo 0
                index[1].postSqlScripts.getValue("test").size shouldBeEqualTo 0
                index[1].descriptions.size shouldBeEqualTo 1
                index[3].postSqlScripts.getValue("test").size shouldBeEqualTo 1
                index[3].postSqlScripts.getValue("test")[0].toString() shouldEndWith "test_post_once.sql"
            }
        }

        on("indexing with a filter") {
            val index = testIndexer.index(testProject) { (_, config) -> config.tags.contains("call3") }

            it("should return a correct index") {
                index.size shouldBeEqualTo 1
                index[0].request!!.toString() shouldEndWith Paths.get("call3/request.xml").toString()
                index[0].response.toString() shouldEndWith Paths.get("call3/response.xml").toString()
            }
        }
    }
})
