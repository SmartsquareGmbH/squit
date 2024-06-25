package de.smartsquare.squit.io

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotExist
import org.junit.jupiter.api.Test
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
class FilesUtilsTest {

    @Test
    fun `getting sorted leaf directories`() {
        val root = createTempDirectory(prefix = "root")
        val child1 = createTempDirectory(prefix = "2-child1", directory = root)
        val child2 = createTempDirectory(prefix = "3-child2", directory = root)
        val child3 = createTempDirectory(prefix = "1-child3", directory = root)
        val grandChild21 = createTempDirectory(prefix = "grandChild21", directory = child2)

        FilesUtils.getLeafDirectories(root).toList() shouldBeEqualTo listOf(child3, child1, grandChild21)
    }

    @Test
    fun `deleting directories recursively if existing`() {
        val root = createTempDirectory(prefix = "root")
        createTempDirectory(prefix = "child1", directory = root)

        FilesUtils.deleteRecursivelyIfExisting(root)

        root.toFile().shouldNotExist()
    }
}
