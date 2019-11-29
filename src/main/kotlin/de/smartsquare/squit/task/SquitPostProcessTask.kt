package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Task for post-processing the responses.
 */
open class SquitPostProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * The directory of the test sources.
     */
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
    @Suppress("UnstableApiUsage")
    @TaskAction
    fun run() {
        val workerQueue = workerExecutor.noIsolation()

        FilesUtils.deleteRecursivelyIfExisting(processedActualResponsesPath)
        Files.createDirectories(processedActualResponsesPath)

        FilesUtils.getLeafDirectories(actualResponsesPath, sort = false).forEach { testPath ->
            workerQueue.submit(SquitPostProcessWorker::class.java) {
                it.processedSourcesPath.set(processedSourcesPath.toFile())
                it.actualResponsesPath.set(actualResponsesPath.toFile())
                it.processedActualResponsesPath.set(processedActualResponsesPath.toFile())
                it.testPath.set(testPath.toFile())
            }
        }
    }
}
