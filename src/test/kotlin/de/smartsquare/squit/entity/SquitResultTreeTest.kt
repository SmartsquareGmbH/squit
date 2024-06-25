package de.smartsquare.squit.entity

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class SquitResultTreeTest {

    @Test
    fun `constructing a SquitResultTree`() {
        val resultList = listOf(
            constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("c")),
            constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("c").resolve("c")),
            constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("d"), isIgnored = true),
            constructTestSquitResult(Paths.get("a"), Paths.get("c"), Paths.get(""), result = "xyz"),
            constructTestSquitResult(Paths.get("x"), Paths.get("y").resolve("z"), Paths.get("x")),
        )

        val resultTrees = SquitResultTree.fromList(resultList)

        resultTrees.size shouldBe 2
        resultTrees.first().name shouldBeEqualTo "a"
        resultTrees.first().successfulTests shouldBe 2
        resultTrees.first().ignoredTests shouldBe 1
        resultTrees.first().failedTests shouldBe 1
        resultTrees.first().totalTests shouldBe 4
        resultTrees.first().isSuccess.shouldBeFalse()
        resultTrees.first().children.size shouldBe 2
        resultTrees.last().name shouldBeEqualTo "x"
        resultTrees.last().children.first().children.first().children.first().name shouldBeEqualTo "x"
        resultTrees.last().children.first().children.first().children.first().successfulTests shouldBe 1
        resultTrees.last().children.first().children.first().children.first().failedTests shouldBe 0
        resultTrees.last().children.first().children.first().children.first().isSuccess.shouldBeTrue()
    }

    @Suppress("LongParameterList")
    private fun constructTestSquitResult(
        contextPath: Path,
        suitePath: Path,
        testDirectoryPath: Path,
        mediaType: MediaType = "application/xml".toMediaType(),
        result: String = "",
        isIgnored: Boolean = false,
    ) = SquitResult(
        0, result, SquitResponseInfo(), isIgnored, mediaType, "",
        contextPath, suitePath, testDirectoryPath, Paths.get(""),
    )
}
