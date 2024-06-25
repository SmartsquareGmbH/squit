package de.smartsquare.squit.task

import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.dir
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.file.Files
import javax.inject.Inject

/**
 * Task for post-processing the responses.
 */
@CacheableTask
abstract class SquitPostProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val processedSourcesDir = project.layout.buildDirectory.dir(SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    @get:Internal
    val actualResponsesDir = project.layout.buildDirectory.dir(SQUIT_DIRECTORY, RESPONSES_DIRECTORY, RAW_DIRECTORY)

    /**
     * Collection of actual response files except meta.json files for up-to-date checking.
     */
    @Suppress("unused")
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val actualResponseFiles: Provider<FileTree> = actualResponsesDir
        .map { dir -> dir.asFileTree.matching { tree -> tree.exclude("**/$META") } }

    /**
     * The directory to save the results in.
     */
    @get:OutputDirectory
    val processedActualResponseDir = project.layout.buildDirectory
        .dir(SQUIT_DIRECTORY, RESPONSES_DIRECTORY, PROCESSED_DIRECTORY)

    init {
        group = "Build"
        description = "Transforms the actual responses to be readable and usable for the comparing task."
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedActualResponseDir.asPath)
        Files.createDirectories(processedActualResponseDir.asPath)

        val workerQueue = workerExecutor.noIsolation()

        FilesUtils.getLeafDirectories(actualResponsesDir.asPath, sort = false).forEach { testPath ->
            workerQueue.submit(Worker::class.java) {
                it.processedSourcesPath.set(processedSourcesDir.get().asFile)
                it.actualResponsesPath.set(actualResponsesDir.get().asFile)
                it.processedActualResponsesPath.set(processedActualResponseDir.get().asFile)
                it.testPath.set(testPath.toFile())
            }
        }
    }

    internal abstract class Worker : WorkAction<WorkerParameters> {
        override fun execute() {
            SquitPostProcessRunner.run(
                processedSourcesPath = parameters.processedSourcesPath.asPath,
                actualResponsesPath = parameters.actualResponsesPath.asPath,
                processedActualResponsesPath = parameters.processedActualResponsesPath.asPath,
                testPath = parameters.testPath.asPath,
            )
        }
    }

    internal interface WorkerParameters : WorkParameters {
        val processedSourcesPath: DirectoryProperty
        val actualResponsesPath: DirectoryProperty
        val processedActualResponsesPath: DirectoryProperty
        val testPath: DirectoryProperty
    }
}
