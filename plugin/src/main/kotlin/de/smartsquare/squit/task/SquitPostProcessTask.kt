package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.SquitPostProcessor
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
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

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val postProcessClassName by lazy { extension.postProcessClass }

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @InputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path, "squit", "sources")

    /**
     * The directory of the previously requested responses.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @InputDirectory
    val actualResponsesPath: Path = Paths.get(project.buildDir.path, "squit", "responses", "raw")

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @OutputDirectory
    val processedActualResponsesPath: Path = Paths.get(project.buildDir.path, "squit", "responses", "processed")

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

            val resultProcessedActualResponsePath = Files.createDirectories(processedActualResponsesPath
                    .resolve(testDir.cut(actualResponsesPath)))

            val resultProcessedActualResponseFilePath = FilesUtils
                    .createFileIfNotExists(resultProcessedActualResponsePath.resolve(ACTUAL_RESPONSE))

            val actualResponse = SAXReader().read(actualResponsePath)
            val expectedResponse = SAXReader().read(expectedResponsePath)

            processor?.process(actualResponse, expectedResponse)

            actualResponse.write(resultProcessedActualResponseFilePath)
        }
    }
}
