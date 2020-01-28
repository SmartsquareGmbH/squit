package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.config.TestIndexer
import de.smartsquare.squit.config.shouldExclude
import de.smartsquare.squit.config.tags
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
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
@CacheableTask
open class SquitPreProcessTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

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

    @get:Nested
    internal var extension by Delegates.notNull<SquitExtension>()

    @get:Internal
    internal val sourceDir by lazy { extension.sourceDir.asPath }

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
        val index = TestIndexer(projectConfig).index(sourceDir, ::filterIndex)

        FilesUtils.deleteRecursivelyIfExisting(processedSourcesPath)
        Files.createDirectories(processedSourcesPath)

        if (GradleVersion.current() >= GradleVersion.version("5.6")) {
            val workerQueue = workerExecutor.noIsolation()

            index.forEach { test ->
                workerQueue.submit(Worker::class.java) {
                    it.sourceDir.set(sourceDir.toFile())
                    it.processedSourcesPath.set(processedSourcesPath.toFile())
                    it.test.set(test)
                }
            }
        } else {
            index.forEach { test ->
                SquitPreProcessRunner.run(sourceDir, processedSourcesPath, test)
            }
        }
    }

    private fun filterIndex(input: Pair<Path, Config>) = when {
        isTestExcluded(input.second, shouldUnexclude) -> {
            logger.info("Excluding test ${input.first.cut(sourceDir)}")

            false
        }
        !isTestCoveredByTags(input.second, tags) -> {
            logger.info("Ignoring test ${input.first.cut(sourceDir)}")

            false
        }
        else -> true
    }

    private fun isTestExcluded(config: Config, shouldUnexclude: Boolean): Boolean {
        return config.shouldExclude && !shouldUnexclude
    }

    private fun isTestCoveredByTags(config: Config, tags: List<String>): Boolean {
        return tags.isEmpty() || tags.any { it in config.tags }
    }

    @Suppress("UnstableApiUsage")
    internal abstract class Worker : WorkAction<WorkerParameters> {

        private val sourceDir get() = parameters.sourceDir.asPath
        private val processedSourcesPath get() = parameters.processedSourcesPath.asPath
        private val test get() = parameters.test.get()

        override fun execute() {
            SquitPreProcessRunner.run(sourceDir, processedSourcesPath, test)
        }
    }

    @Suppress("UnstableApiUsage")
    internal interface WorkerParameters : WorkParameters {
        val sourceDir: DirectoryProperty
        val processedSourcesPath: DirectoryProperty
        val test: Property<SquitTest>
    }
}
