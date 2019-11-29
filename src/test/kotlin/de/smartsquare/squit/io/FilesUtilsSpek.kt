package de.smartsquare.squit.io

import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotExist
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object FilesUtilsSpek : Spek({
    given("a directory structure") {
        on("getting sorted leaf directories") {
            val root = createTempDir(prefix = "root").toPath()
            val child1 = createTempDir(prefix = "2-child1", directory = root.toFile()).toPath()
            val child2 = createTempDir(prefix = "3-child2", directory = root.toFile()).toPath()
            val child3 = createTempDir(prefix = "1-child3", directory = root.toFile()).toPath()
            val grandChild21 = createTempDir(prefix = "grandChild21", directory = child2.toFile()).toPath()

            it("should return a correct list") {
                FilesUtils.getLeafDirectories(root).toList() shouldEqual listOf(child3, child1, grandChild21)
            }
        }

        on("deleting directories recursively if existing") {
            val root = createTempDir(prefix = "root").toPath()
            createTempDir(prefix = "child1", directory = root.toFile()).toPath()

            it("should delete all directories") {
                FilesUtils.deleteRecursivelyIfExisting(root)

                root.toFile().shouldNotExist()
            }
        }
    }
})
