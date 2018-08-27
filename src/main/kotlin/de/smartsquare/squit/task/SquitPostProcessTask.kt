package de.smartsquare.squit.task

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.mediaType
import org.gradle.api.DefaultTask
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
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @InputDirectory
    val processedSourcesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY,
        SOURCES_DIRECTORY
    )

    /**
     * The directory of the previously requested responses.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @InputDirectory
    val actualResponsesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY,
        RESPONSES_DIRECTORY,
        RAW_DIRECTORY
    )

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @OutputDirectory
    val processedActualResponsesPath: Path = Paths.get(
        project.buildDir.path,
        SQUIT_DIRECTORY,
        RESPONSES_DIRECTORY,
        PROCESSED_DIRECTORY
    )

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

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
            val resultActualResponsePath = Files.createDirectories(
                processedActualResponsesPath.resolve(testDir.cut(actualResponsesPath))
            )

            val errorFile = testDir.resolve(ERROR)

            if (Files.exists(errorFile)) {
                Files.copy(errorFile, resultActualResponsePath.resolve(ERROR))
            } else {
                val configPath = FilesUtils.validateExistence(
                    processedSourcesPath.resolve(testDir.cut(actualResponsesPath))
                )
                    .resolve(CONFIG)

                val config = ConfigFactory.parseFile(configPath.toFile())

                val actualResponsePath = FilesUtils.validateExistence(
                    testDir.resolve(MediaTypeFactory.actualResponse(config.mediaType))
                )

                val expectedResponsePath = FilesUtils.validateExistence(
                    processedSourcesPath
                        .resolve(testDir.cut(actualResponsesPath))
                        .resolve(MediaTypeFactory.expectedResponse(config.mediaType))
                )

                val resultActualResponseFilePath = resultActualResponsePath
                    .resolve(MediaTypeFactory.actualResponse(config.mediaType))

                try {
                    MediaTypeFactory.processor(config.mediaType)
                        .postProcess(actualResponsePath, expectedResponsePath, resultActualResponseFilePath, config)
                } catch (error: Throwable) {
                    Files.write(resultActualResponsePath.resolve(ERROR), error.toString().toByteArray())
                }
            }
        }
    }
}
