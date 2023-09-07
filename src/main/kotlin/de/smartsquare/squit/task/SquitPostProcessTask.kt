package de.smartsquare.squit.task

import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.META
import de.smartsquare.squit.util.Constants.PROCESSED_DIRECTORY
import de.smartsquare.squit.util.Constants.RAW_DIRECTORY
import de.smartsquare.squit.util.Constants.RESPONSES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

/**
 * Task for post-processing the responses.
 */
@CacheableTask
open class SquitPostProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    val processedSourcesPath: Path = Paths.get(
        project.layout.buildDirectory.get().asFile.path,
        SQUIT_DIRECTORY,
        SOURCES_DIRECTORY,
    )

    /**
     * The directory of the previously requested responses.
     */
    @Internal
    val actualResponsesPath: Path = Paths.get(
        project.layout.buildDirectory.get().asFile.path,
        SQUIT_DIRECTORY,
        RESPONSES_DIRECTORY,
        RAW_DIRECTORY,
    )

    /**
     * Collection of actual response files except meta.json files for up-to-date checking.
     */
    @Suppress("unused")
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val actualResponsesFiles: ConfigurableFileTree = project
        .fileTree(actualResponsesPath) { it.exclude("**/$META") }

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    val processedActualResponsesPath: Path = Paths.get(
        project.layout.buildDirectory.get().asFile.path,
        SQUIT_DIRECTORY,
        RESPONSES_DIRECTORY,
        PROCESSED_DIRECTORY,
    )

    init {
        group = "Build"
        description = "Transforms the actual responses to be readable and usable for the comparing task."
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedActualResponsesPath)
        Files.createDirectories(processedActualResponsesPath)

        if (GradleVersion.current() >= GradleVersion.version("5.6")) {
            val workerQueue = workerExecutor.noIsolation()

            FilesUtils.getLeafDirectories(actualResponsesPath, sort = false).forEach { testPath ->
                workerQueue.submit(Worker::class.java) {
                    it.processedSourcesPath.set(processedSourcesPath.toFile())
                    it.actualResponsesPath.set(actualResponsesPath.toFile())
                    it.processedActualResponsesPath.set(processedActualResponsesPath.toFile())
                    it.testPath.set(testPath.toFile())
                }
            }
        } else {
            FilesUtils.getLeafDirectories(actualResponsesPath, sort = false).forEach { testPath ->
                SquitPostProcessRunner.run(
                    processedSourcesPath,
                    actualResponsesPath,
                    processedActualResponsesPath,
                    testPath,
                )
            }
        }
    }

    internal abstract class Worker : WorkAction<WorkerParameters> {

        private val processedSourcesPath get() = parameters.processedSourcesPath.asPath
        private val actualResponsesPath get() = parameters.actualResponsesPath.asPath
        private val processedActualResponsesPath get() = parameters.processedActualResponsesPath.asPath
        private val testPath get() = parameters.testPath.asPath

        override fun execute() {
            SquitPostProcessRunner.run(
                processedSourcesPath,
                actualResponsesPath,
                processedActualResponsesPath,
                testPath,
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
