package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.config.TestIndexer
import de.smartsquare.squit.config.shouldExclude
import de.smartsquare.squit.config.tags
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.dir
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

/**
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 */
@CacheableTask
abstract class SquitPreProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * The path the sources lie in.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    /**
     * The directory to save the results in.
     */
    @get:OutputDirectory
    val processedSources = project.layout.buildDirectory.dir(SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    /**
     * The tags to filter by (and).
     */
    @get:Input
    val tagsAnd: Provider<Set<String>> = project.provider {
        getTags("tags") + getTags("tagsAnd")
    }

    /**
     * The tags to filter by (or).
     */
    @get:Input
    val tagsOr: Provider<Set<String>> = project.provider { getTags("tagsOr") }

    /**
     * If all excluded or ignored tests should be run nevertheless.
     */
    @get:Input
    val shouldUnexclude: Provider<Boolean> = project.provider { project.properties.containsKey("unexclude") }

    /**
     * The properties of the project parsed into a [Config] object.
     */
    @get:Input
    val projectConfig: Provider<Config> = project.provider {
        ConfigValueFactory
            .fromMap(
                project.properties
                    .filterKeys { it is String && it.startsWith("squit.") }
                    .mapKeys { (key, _) -> key.replaceFirst("squit.", "") },
            )
            .toConfig()
    }

    init {
        group = "Build"
        description = "Transforms the sources to be readable and usable for the following tasks."
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        val index = TestIndexer(projectConfig.get()).index(sourceDir.asPath) { filterIndex(it) }

        FilesUtils.deleteRecursivelyIfExisting(processedSources.asPath)
        Files.createDirectories(processedSources.asPath)

        val workerQueue = workerExecutor.noIsolation()

        index.forEach { test ->
            workerQueue.submit(Worker::class.java) {
                it.sourceDir.set(sourceDir.get().asFile)
                it.processedSourcesPath.set(processedSources.get().asFile)
                it.test.set(test)
            }
        }
    }

    private fun getTags(name: String): Set<String> {
        val tagsString = if (project.hasProperty(name)) project.property(name)?.toString() else null

        return tagsString
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun filterIndex(input: Pair<Path, Config>) = when {
        isTestExcluded(input.second) -> {
            logger.info("Excluding test ${input.first.cut(sourceDir.asPath)}")

            false
        }

        !isTestCoveredByTags(input.second) -> {
            logger.info("Ignoring test ${input.first.cut(sourceDir.asPath)}")

            false
        }

        else -> true
    }

    private fun isTestExcluded(config: Config): Boolean = config.shouldExclude && !shouldUnexclude.get()

    private fun isTestCoveredByTags(config: Config): Boolean = (
        tagsAnd.get().isEmpty() ||
            tagsAnd.get().all {
                it in config.tags
            }
        ) &&
        (tagsOr.get().isEmpty() || tagsOr.get().any { it in config.tags })

    internal abstract class Worker : WorkAction<WorkerParameters> {
        override fun execute() {
            SquitPreProcessRunner.run(
                sourceDir = parameters.sourceDir.asPath,
                processedSourcesPath = parameters.processedSourcesPath.asPath,
                test = parameters.test.get(),
            )
        }
    }

    internal interface WorkerParameters : WorkParameters {
        val sourceDir: DirectoryProperty
        val processedSourcesPath: DirectoryProperty
        val test: Property<SquitTest>
    }
}
