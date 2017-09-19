package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.SquitPostProcessor
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.write
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * Task for post-processing the responses.
 *
 * @author Ruben Gees
 */
open class SquitPostProcessTask : DefaultTask() {

    /**
     * The class name of the [SquitPostProcessor] to use.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val postProcessClassName by lazy { extension.postProcessorClass }

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @InputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The directory of the previously requested responses.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @InputDirectory
    val actualResponsesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @OutputDirectory
    val processedActualResponsesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, RESPONSES_DIRECTORY, PROCESSED_DIRECTORY)

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    @get:Internal
    private val processor by lazy {
        postProcessClassName?.let {
            if (it.isNotBlank()) {
                logger.info("Using $it for post processing.")

                Class.forName(it).newInstance() as SquitPostProcessor
            } else {
                null
            }
        }
    }

    init {
        group = "Build"
        description = "Transforms the actual responses to be readable and usable for the comparing task."
    }

    /**
     * Runs the task.
     */
    @Suppress("unused")
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedActualResponsesPath)
        Files.createDirectories(processedActualResponsesPath)

        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { testDir ->
            val actualResponsePath = FilesUtils.validateExistence(testDir.resolve(ACTUAL_RESPONSE))
            val expectedResponsePath = FilesUtils.validateExistence(processedSourcesPath
                    .resolve(testDir.cut(actualResponsesPath))
                    .resolve(EXPECTED_RESPONSE))

            val resultActualResponsePath = Files.createDirectories(processedActualResponsesPath
                    .resolve(testDir.cut(actualResponsesPath)))

            val resultActualResponseFilePath = FilesUtils.createFileIfNotExists(resultActualResponsePath
                    .resolve(ACTUAL_RESPONSE))

            val actualResponse = SAXReader().read(actualResponsePath)
            val expectedResponse = SAXReader().read(expectedResponsePath)

            processor?.process(actualResponse, expectedResponse)

            actualResponse.write(resultActualResponseFilePath)
        }
    }
}
