package de.smartsquare.squit.entity

import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Intermediate data class for storing test results.
 *
 * @property id A unique id to use for further processing.
 * @property result The result of the test. An empty String means the test was successful, otherwise it contains the
 * XMLUnit diff.
 * @property isIgnored If this result is ignored.
 * @param contextPath The path of the context the test has been run in. This means the test path without the suite
 * and the actual test directory.
 * @param suitePath The path of the suite the test has been run in. This means the parent directory of the test.
 * @param testDirectoryPath The path of the directory, the test files are contained in.
 * @param squitBuildDirectoryPath The directory of Squit's build files. Used to resolve the [metaInfo],
 * [expectedLines] and [actualLines] properties.
 *
 * @author Ruben Gees
 */
data class SquitResult(
        val id: Long,
        val result: String,
        val isIgnored: Boolean,
        private val contextPath: Path,
        private val suitePath: Path,
        private val testDirectoryPath: Path,
        private val squitBuildDirectoryPath: Path
) {

    /**
     * Convenience property being true if the test was successful.
     */
    val isSuccess = result.isBlank()

    /**
     * Convenience property being true if this result is an error.
     * This differs from a failure as such an exception has been thrown.
     */
    val isError by lazy { Files.exists(errorPath) }

    /**
     * Convenience property with the name of this test.
     *
     * The name is denoted by the filename of the directory it resides in.
     */
    val name = testDirectoryPath.fileName.toString()

    /**
     * Convenience property with the path to the test without the actual test directory.
     */
    val path: Path = contextPath.resolve(suitePath)

    /**
     * Convenience property with the full path consisting of the [path] and the actual test directory.
     */
    val fullPath: Path = path.resolve(testDirectoryPath)

    /**
     * Additional information associated with this result.
     */
    val metaInfo: SquitMetaInfo by lazy {
        SquitMetaInfo.fromJson(Files.readAllBytes(metaInfoPath).toString(Charset.defaultCharset()))
    }

    /**
     * [List] of lines of the expected response.
     */
    val expectedLines: List<String> by lazy {
        if (isError) {
            Files.readAllLines(errorPath)
        } else {
            Files.readAllLines(expectedResponsePath)
        }
    }

    /**
     * [List] of lines of the actual response.
     */
    val actualLines: List<String> by lazy {
        if (isError) {
            Files.readAllLines(errorPath)
        } else {
            Files.readAllLines(actualResponsePath)
        }
    }

    private val metaInfoPath = squitBuildDirectoryPath
            .resolve(RESPONSES_DIRECTORY)
            .resolve(RAW_DIRECTORY)
            .resolve(fullPath)
            .resolve(META)

    private val expectedResponsePath = squitBuildDirectoryPath
            .resolve(SOURCES_DIRECTORY)
            .resolve(fullPath)
            .resolve(EXPECTED_RESPONSE)

    private val actualResponsePath = squitBuildDirectoryPath
            .resolve(RESPONSES_DIRECTORY)
            .resolve(PROCESSED_DIRECTORY)
            .resolve(fullPath)
            .resolve(ACTUAL_RESPONSE)

    private val errorPath = squitBuildDirectoryPath
            .resolve(RESPONSES_DIRECTORY)
            .resolve(PROCESSED_DIRECTORY)
            .resolve(fullPath)
            .resolve(ERROR)

    /**
     * Returns a copy of this result with the first part of the [fullPath] cut.
     */
    @Suppress("DataClassContainsFunctions")
    fun cutFirstPathElement(): SquitResult {
        val isContextPathEmpty = contextPath.fileName.toString().isBlank()
        val isSquitPathEmpty = suitePath.fileName.toString().isBlank()

        val newContextPath = contextPath.drop(1)
                .fold(Paths.get(""), { acc, path -> acc.resolve(path) })

        val newSuitePath = if (isContextPathEmpty) {
            suitePath.drop(1)
                    .fold(Paths.get(""), { acc, path -> acc.resolve(path) })
        } else {
            suitePath
        }

        val newTestDirectoryPath = if (isContextPathEmpty && isSquitPathEmpty) {
            testDirectoryPath.drop(1)
                    .fold(Paths.get(""), { acc, path -> acc.resolve(path) })
        } else {
            testDirectoryPath
        }

        return copy(contextPath = newContextPath, suitePath = newSuitePath, testDirectoryPath = newTestDirectoryPath)
    }
}
