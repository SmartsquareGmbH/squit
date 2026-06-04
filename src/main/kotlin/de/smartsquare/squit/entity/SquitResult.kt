package de.smartsquare.squit.entity

import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE_INFO
import de.smartsquare.squit.util.Constants.DESCRIPTION
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import okhttp3.MediaType
import java.nio.file.Files
import java.nio.file.Path

/**
 * Intermediate data class for storing test results.
 *
 * @property id A unique id to use for further processing.
 * @property difference The difference of the expected and actual responses. If empty, the test was successful.
 * @property expectedResponseInfo The response info object, which is expected in this test.
 * @property isIgnored If this result is ignored.
 * @property mediaType The media type of the associated request and response.
 * @param contextPath The path of the context the test has been run in. This means the test path without the suite
 * and the actual test directory.
 * @param suitePath The path of the suite the test has been run in. This means the parent directory of the test.
 * @param testDirectoryPath The path of the directory, the test files are contained in.
 * @param squitBuildDirectoryPath The directory of Squit's build files. Used to resolve the [metaInfo],
 * [expectedContent] and [actualContent] methods.
 */
data class SquitResult(
    val id: Long,
    val difference: String,
    val expectedResponseInfo: SquitResponseInfo,
    val isIgnored: Boolean,
    val mediaType: MediaType,
    val alternativeName: String,
    private val contextPath: Path,
    private val suitePath: Path,
    private val testDirectoryPath: Path,
    private val squitBuildDirectoryPath: Path,
) {

    /**
     * Convenience property being true if the test was successful.
     */
    val isSuccess = difference.isBlank()

    /**
     * Convenience property being true if this result is an error.
     * This differs from a failure as such an exception has been thrown.
     */
    val isError by lazy { Files.exists(errorPath) }

    /**
     * Convenience property with the name of this test.
     *
     * The name is denoted by the filename of the directory it resides in and optionally
     * by a title set in the corresponding test.conf file.
     */
    val simpleName = testDirectoryPath.fileName.toString()

    /**
     * An optional description of the test.
     */
    val description by lazy {
        val descriptionPath = sourceDir.resolve(DESCRIPTION)

        if (Files.exists(descriptionPath)) {
            FilesUtils.readAllBytes(descriptionPath).toString(Charsets.UTF_8)
        } else {
            null
        }
    }

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
        SquitMetaInfo.fromJson(FilesUtils.readAllBytes(metaInfoPath).toString(Charsets.UTF_8))
    }

    /**
     * Returns [List] of lines of the expected response.
     */
    fun expectedContent() = if (isError) {
        FilesUtils.readString(errorPath)
    } else {
        FilesUtils.readString(expectedResponsePath)
    }

    /**
     * Returns [List] of lines of the actual response.
     */
    fun actualContent() = if (isError) {
        FilesUtils.readString(errorPath)
    } else {
        FilesUtils.readString(actualResponsePath)
    }

    /**
     * Returns [List] of lines of the actual info response.
     */
    fun actualInfoContent() = if (Files.exists(actualResponseInfoPath)) {
        FilesUtils.readString(actualResponseInfoPath)
    } else {
        ""
    }

    private val metaInfoPath = squitBuildDirectoryPath
        .resolve(RESPONSES_DIRECTORY)
        .resolve(RAW_DIRECTORY)
        .resolve(fullPath)
        .resolve(META)

    private val sourceDir = squitBuildDirectoryPath
        .resolve(SOURCES_DIRECTORY)
        .resolve(fullPath)

    private val expectedResponsePath = sourceDir
        .resolve(MediaTypeFactory.expectedResponse(mediaType))

    private val actualResponsePath = squitBuildDirectoryPath
        .resolve(RESPONSES_DIRECTORY)
        .resolve(PROCESSED_DIRECTORY)
        .resolve(fullPath)
        .resolve(MediaTypeFactory.actualResponse(mediaType))

    private val errorPath = squitBuildDirectoryPath
        .resolve(RESPONSES_DIRECTORY)
        .resolve(PROCESSED_DIRECTORY)
        .resolve(fullPath)
        .resolve(ERROR)

    private val actualResponseInfoPath = squitBuildDirectoryPath
        .resolve(RESPONSES_DIRECTORY)
        .resolve(RAW_DIRECTORY)
        .resolve(fullPath)
        .resolve(ACTUAL_RESPONSE_INFO)
}
