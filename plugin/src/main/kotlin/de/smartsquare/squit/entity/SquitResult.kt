package de.smartsquare.squit.entity

import java.nio.file.Path

/**
 * Intermediate data class for storing test results.
 *
 * @property contextPath The path of the context the test has been run in. This means the test path without the suite and
 *                   the actual test directory.
 * @property suitePath The path of the suite the test has been run in. This means the parent directory of the test.
 * @property testDirectoryPath The path of the directory, the test files are contained in.
 * @property result The result of the test. An empty String means the test was successful, otherwise it contains the#
 *                  XMLUnit diff.
 *
 * @author Ruben Gees
 */
data class SquitResult(private val contextPath: Path, private val suitePath: Path, val testDirectoryPath: Path,
                       val result: String = "") {

    /**
     * Convenience property being true if the test was successful.
     */
    val isSuccess = result.isBlank()

    /**
     * Convenience property consisting of the [contextPath] and the [suitePath].
     */
    val path: Path get() = contextPath.resolve(suitePath)

    /**
     * Convenience property consisting of the [path] and the [testDirectoryPath].
     */
    val fullPath: Path get() = path.resolve(testDirectoryPath)
}
