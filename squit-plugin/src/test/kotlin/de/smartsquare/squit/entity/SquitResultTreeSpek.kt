package de.smartsquare.squit.entity

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Ruben Gees
 */
object SquitResultTreeSpek : Spek({

    given("a list of SquitResults") {
        fun constructTestSquitResult(
                contextPath: Path,
                suitePath: Path,
                testDirectoryPath: Path,
                result: String = ""
        ) = SquitResult(0, result, contextPath, suitePath, testDirectoryPath, Paths.get(""))

        val resultList = listOf(
                constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("c")),
                constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("c").resolve("c")),
                constructTestSquitResult(Paths.get("a"), Paths.get("b"), Paths.get("d")),
                constructTestSquitResult(Paths.get("a"), Paths.get("c"), Paths.get(""), "xyz"),
                constructTestSquitResult(Paths.get("x"), Paths.get("y").resolve("z"), Paths.get("x"))
        )

        on("constructing a SquitResultTree") {
            val resultTrees = SquitResultTree.fromList(resultList)

            it("should contain correct values") {
                resultTrees.size shouldBe 2

                resultTrees.first().name shouldEqualTo "a"
                resultTrees.first().successfulTests shouldBe 3
                resultTrees.first().failedTests shouldBe 1
                resultTrees.first().totalTests shouldBe 4
                resultTrees.first().isSuccess shouldBe false
                resultTrees.first().children.size shouldBe 2

                resultTrees.last().name shouldEqualTo "x"
                resultTrees.last().children.first().children.first().children.first().name shouldEqualTo "x"
                resultTrees.last().children.first().children.first().children.first().successfulTests shouldBe 1
                resultTrees.last().children.first().children.first().children.first().failedTests shouldBe 0
                resultTrees.last().children.first().children.first().children.first().isSuccess shouldBe true
            }
        }
    }
})