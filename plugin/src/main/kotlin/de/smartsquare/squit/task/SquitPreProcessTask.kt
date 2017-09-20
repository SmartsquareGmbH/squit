package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.SquitPreProcessor
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.REQUEST
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCE_RESPONSE
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.safeStore
import de.smartsquare.squit.util.write
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.properties.Delegates

/**
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 *
 * @author Ruben Gees
 */
@Suppress("LargeClass")
open class SquitPreProcessTask : DefaultTask() {

    /**
     * The class name of the [SquitPreProcessor] to use.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val preProcessorClassName by lazy { extension.preProcessorClass }

    /**
     * The path of a groovy script to use for pre processing.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputFile
    @get:Optional
    val preProcessorScriptPath by lazy { extension.preProcessorScriptPath }

    /**
     * The tags to filter by.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val tags = when (project.hasProperty(SquitProperties.TAGS_PROPERTY)) {
        true -> project.property(SquitProperties.TAGS_PROPERTY) as String?
        false -> null
    }?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val sourcesPath by lazy {
        extension.sourcesPath ?: throw IllegalArgumentException("sourcesPath cannot be null")
    }

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    @get:Internal
    private val processor by lazy {
        preProcessorClassName?.let {
            if (it.isNotBlank()) {
                logger.info("Using $it for pre processing.")

                Class.forName(it).newInstance() as SquitPreProcessor
            } else {
                null
            }
        }
    }

    @get:Internal
    private val processorScript by lazy {
        preProcessorScriptPath?.let {
            if (!Files.exists(it)) {
                throw GradleException("Could not find pre processor script: $it")
            } else {
                GroovyShell().parse(Files.newBufferedReader(it))
            }
        }
    }

    @get:Internal
    private val propertyCache = mutableMapOf<Path, SquitProperties>()

    init {
        group = "Build"
        description = "Transforms the sources to be readable and usable for the following tasks."
    }

    /**
     * Runs the task.
     */
    @Suppress("unused")
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedSourcesPath)
        Files.createDirectories(processedSourcesPath)

        FilesUtils.getSortedLeafDirectories(sourcesPath).forEach {
            val resolvedProperties = resolveProperties(it)

            if (shouldRunTest(resolvedProperties)) {
                val (requestPath, responsePath, sqlFilePaths) = getRelevantPathsForTest(it, resolvedProperties)
                val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(it.cut(sourcesPath)))

                val processedPropertiesPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(CONFIG))

                val processedRequestPath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(REQUEST))
                val processedResponsePath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(EXPECTED_RESPONSE))

                val request = SAXReader().read(requestPath)
                val response = SAXReader().read(responsePath)

                processor?.process(request, response)
                processorScript
                        ?.apply {
                            binding = Binding(mapOf(
                                    "request" to request,
                                    "expectedResponse" to response
                            ))
                        }
                        ?.run()

                request.write(processedRequestPath)
                response.write(processedResponsePath)

                sqlFilePaths.forEach { Files.copy(it, processedResultPath.resolve(it.fileName), REPLACE_EXISTING) }

                resolvedProperties.writeToProperties().safeStore(processedPropertiesPath)
            } else {
                logger.warn("Ignoring test ${it.cut(sourcesPath)}")
            }
        }
    }

    private fun resolveProperties(testPath: Path): SquitProperties {
        var currentDirectoryPath = testPath
        val result = SquitProperties()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(CONFIG).also { propertiesPath ->
                if (Files.exists(propertiesPath)) {
                    val newProperties = propertyCache.getOrPut(propertiesPath, {
                        SquitProperties().fillFromProperties(propertiesPath, project.properties)
                    })

                    result.mergeWith(newProperties)
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.validateAndGetErrorMessage().let {
            when (it) {
                null -> result
                else -> throw GradleException("Invalid $CONFIG file on path of test: " +
                        "${testPath.cut(sourcesPath)} ($it)")
            }
        }
    }

    private fun getRelevantPathsForTest(testPath: Path, properties: SquitProperties): Triple<Path, Path, List<Path>> {
        var requestPath: Path? = null
        var responsePath: Path? = null
        val sqlFilePaths = mutableListOf<Path>()

        Files.list(testPath).use {
            it.sequential().forEach { path ->
                when (path.fileName.toString()) {
                    REQUEST -> requestPath = path
                    SOURCE_RESPONSE -> responsePath = path
                    CONFIG -> Unit
                    in properties.databaseConfigurations.map { "${it.name}_pre.sql" } -> sqlFilePaths.add(path)
                    in properties.databaseConfigurations.map { "${it.name}_post.sql" } -> sqlFilePaths.add(path)
                    else -> logger.warn("Ignoring unknown file ${path.fileName}")
                }
            }
        }

        requestPath?.let { safeRequestFile ->
            responsePath?.let { safeResponseFile ->
                return Triple(safeRequestFile, safeResponseFile, sqlFilePaths)
            }

            throw GradleException("Missing $SOURCE_RESPONSE for test: ${testPath.fileName}")
        }

        throw GradleException("Missing $REQUEST for test: ${testPath.fileName}")
    }

    private fun shouldRunTest(properties: SquitProperties): Boolean {
        return !properties.ignore && (tags.isEmpty() || tags.any { it in properties.tags })
    }
}
