package de.smartsquare.squit.task

import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.SquitPreProcessor
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants
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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 *
 * @author Ruben Gees
 */
@Suppress("LargeClass")
open class SquitPreProcessTask : DefaultTask() {

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
    private val leafDirectoriesWithProperties by lazy {
        FilesUtils.getSortedLeafDirectories(sourcesPath)
                .map { it to resolveProperties(it) }
                .filter { (testPath, resolvedProperties) ->
                    when {
                        isTestIgnored(resolvedProperties) -> {
                            logger.warn("Excluding test ${testPath.cut(sourcesPath)}")

                            false
                        }
                        !isTestCoveredByTags(resolvedProperties) -> false
                        else -> true
                    }
                }
    }

    @get:Internal
    private val propertyCache = mutableMapOf<Path, SquitProperties>()

    @get:Internal
    private val pathCache = mutableMapOf<Path, List<Path>>()

    init {
        group = "Build"
        description = "Transforms the sources to be readable and usable for the following tasks."
    }

    /**
     * Runs the task.
     */
    @Suppress("unused", "NestedBlockDepth")
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedSourcesPath)
        Files.createDirectories(processedSourcesPath)

        leafDirectoriesWithProperties.forEach { (testPath, resolvedProperties) ->
            val requestPath = FilesUtils.validateExistence(testPath.resolve(REQUEST))
            val responsePath = FilesUtils.validateExistence(testPath.resolve(SOURCE_RESPONSE))
            val resolvedSqlScripts = resolveSqlScripts(testPath, resolvedProperties)

            val processedResultPath = Files.createDirectories(processedSourcesPath
                    .resolve(testPath.cut(sourcesPath)))
            val processedPropertiesPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(CONFIG))
            val processedRequestPath = FilesUtils.createFileIfNotExists(processedResultPath
                    .resolve(REQUEST))
            val processedResponsePath = FilesUtils.createFileIfNotExists(processedResultPath
                    .resolve(EXPECTED_RESPONSE))

            val request = SAXReader().read(requestPath)
            val response = SAXReader().read(responsePath)

            resolvedProperties.preProcessors.forEach {
                val preProcessor = Class.forName(it).newInstance() as SquitPreProcessor

                preProcessor.process(request, response)
            }

            resolvedProperties.preProcessorScripts.forEach {
                GroovyShell(javaClass.classLoader).parse(Files.newBufferedReader(it)).apply {
                    binding = Binding(mapOf(
                            "request" to request,
                            "expectedResponse" to response
                    ))
                }.run()
            }

            request.write(processedRequestPath)
            response.write(processedResponsePath)

            resolvedSqlScripts.forEach { (name, content) ->
                Files.write(processedResultPath.resolve(name), content.toByteArray())
            }

            resolvedProperties.writeToProperties().safeStore(processedPropertiesPath)
        }
    }

    private fun resolveProperties(testPath: Path): SquitProperties {
        var currentDirectoryPath = testPath
        val result = SquitProperties()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(Constants.CONFIG).also { propertiesPath ->
                val newProperties = propertyCache.getOrPut(propertiesPath, {
                    SquitProperties().fillFromProperties(propertiesPath, project.properties)
                })

                result.mergeWith(newProperties)
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.validateAndGetErrorMessage().let { errorMessage ->
            when (errorMessage) {
                null -> result
                else -> throw GradleException("Invalid ${Constants.CONFIG} file on path of test: " +
                        "${testPath.cut(sourcesPath)} ($errorMessage)")
            }
        }
    }

    private fun resolveSqlScripts(
            testPath: Path,
            properties: SquitProperties
    ): List<Pair<String, String>> {
        val result = mutableMapOf<String, String>()
        var currentDirectoryPath = testPath

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            val leafsFromHere = pathCache.getOrPut(currentDirectoryPath, {
                leafDirectoriesWithProperties
                        .filter { (path, _) -> path.startsWith(currentDirectoryPath) }
                        .map { (path, _) -> path }
            })

            properties.databaseConfigurations.forEach {
                val preName = "${it.name}_pre.sql"
                val postName = "${it.name}_post.sql"
                val preOnceName = "${it.name}_pre_once.sql"
                val postOnceName = "${it.name}_post_once.sql"

                val prePath = currentDirectoryPath.resolve(preName)
                val preOncePath = currentDirectoryPath.resolve(preOnceName)
                val postPath = currentDirectoryPath.resolve(postName)
                val postOncePath = currentDirectoryPath.resolve(postOnceName)

                if (Files.exists(prePath)) {
                    val content = Files.readAllBytes(prePath).toString(Charsets.UTF_8)

                    result.put(preName, content + result.getOrDefault(preName, ""))
                }

                if (Files.exists(postPath)) {
                    val content = Files.readAllBytes(postPath).toString(Charsets.UTF_8)

                    result.put(postName, result.getOrDefault(postName, "") + content)
                }

                if (Files.exists(preOncePath) && leafsFromHere.indexOf(testPath) == 0) {
                    val content = Files.readAllBytes(preOncePath).toString(Charsets.UTF_8)

                    result.put(preName, content + result.getOrDefault(preName, ""))
                }

                if (Files.exists(postOncePath) && leafsFromHere.indexOf(testPath) == leafsFromHere.lastIndex) {
                    val content = Files.readAllBytes(postOncePath).toString(Charsets.UTF_8)

                    result.put(postName, result.getOrDefault(postName, "") + content)
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.toList()
    }

    private fun isTestIgnored(properties: SquitProperties) = properties.exclude
            && !project.properties.containsKey("unignore")

    private fun isTestCoveredByTags(properties: SquitProperties) = tags.isEmpty() || tags.any { it in properties.tags }
}
