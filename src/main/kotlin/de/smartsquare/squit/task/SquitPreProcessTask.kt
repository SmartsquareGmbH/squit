package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.config.ConfigResolver
import de.smartsquare.squit.config.ConfigWalker
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
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
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 */
@Suppress("LargeClass")
open class SquitPreProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @get:InputDirectory
    val sourcesPath by lazy { extension.sourcesPath }

    /**
     * The directory to save the results in.
     */
    @get:OutputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path, SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The tags to filter by.
     */
    @get:Input
    val tags = when (project.hasProperty("tags")) {
        true -> project.property("tags") as String?
        false -> null
    }?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    /**
     * If all excluded or ignored tests should be run nevertheless.
     */
    @get:Input
    val shouldUnexclude by lazy { project.properties.containsKey("unexclude") }

    /**
     * The properties of the project parsed into a [Config] object.
     */
    @get:Input
    val projectConfig: Config by lazy {
        ConfigValueFactory
            .fromMap(
                project.properties
                    .filterKeys { it is String && it.startsWith("squit.") }
                    .mapKeys { (key, _) -> key.replaceFirst("squit.", "") }
            )
            .toConfig()
    }

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    private val leafDirectoriesWithConfig by lazy {
        configResolver.resolveWithLeafDirectories(tags, shouldUnexclude)
    }

    private val configResolver by lazy {
        ConfigResolver(ConfigWalker(projectConfig, sourcesPath), sourcesPath, logger)
    }

    init {
        group = "Build"
        description = "Transforms the sources to be readable and usable for the following tasks."
    }

    /**
     * Runs the task.
     */
    @Suppress("UnstableApiUsage")
    @TaskAction
    fun run() {
        val workerQueue = workerExecutor.noIsolation()

        FilesUtils.deleteRecursivelyIfExisting(processedSourcesPath)
        Files.createDirectories(processedSourcesPath)

        leafDirectoriesWithConfig.forEach { (testPath, resolvedConfig) ->
            if (testPath.cut(sourcesPath).toList().size < 2) {
                throw GradleException(
                    "Invalid project structure. Please add a project directory to the src/test directory."
                )
            }

            workerQueue.submit(SquitPreProcessWorker::class.java) {
                it.sourcesPath.set(sourcesPath.toFile())
                it.processedSourcesPath.set(processedSourcesPath.toFile())
                it.testPath.set(testPath.toFile())
                it.resolvedConfig.set(resolvedConfig)
            }
        }
    }
}
