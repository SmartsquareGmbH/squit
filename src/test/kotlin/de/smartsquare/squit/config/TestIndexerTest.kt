package de.smartsquare.squit.config

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.TestUtils
import java.nio.file.Paths
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldHaveKey
import org.junit.jupiter.api.Test

class TestIndexerTest {

    private val testProjectRoot = TestUtils.getResourcePath("test-project")
    private val testProject = testProjectRoot.resolve("src/squit")

    private val testIndexer = TestIndexer(
        ConfigFactory.parseMap(
            mapOf(
                "rootDir" to "$testProjectRoot",
                "endpointPlaceholder" to "https://example.com"
            )
        )
    )

    @Test
    fun `should return a correct index`() {
        val index = testIndexer.index(testProject) { true }

        index.size shouldBeEqualTo 4
        index[0].path.toString() shouldEndWith "call1"
        index[0].config.getString("testDir") shouldEndWith "test-project/src/squit/project/call1"
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

    @Test
    fun `should filter correctly`() {
        val index = testIndexer.index(testProject) { (_, config) -> config.tags.contains("call3") }

        index.size shouldBeEqualTo 1
        index[0].request!!.toString() shouldEndWith Paths.get("call3/request.xml").toString()
        index[0].response.toString() shouldEndWith Paths.get("call3/response.xml").toString()
    }
}
