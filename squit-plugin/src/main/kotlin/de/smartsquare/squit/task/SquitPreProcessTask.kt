package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.SquitPreProcessor
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.REQUEST
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SOURCE_RESPONSE
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.databaseConfigurations
import de.smartsquare.squit.util.mergeTag
import de.smartsquare.squit.util.preProcessorScripts
import de.smartsquare.squit.util.preProcessors
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.shouldExclude
import de.smartsquare.squit.util.tags
import de.smartsquare.squit.util.validate
import de.smartsquare.squit.util.write
import de.smartsquare.squit.util.writeTo
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
import java.io.File
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
    val tags = when (project.hasProperty("tags")) {
        true -> project.property("tags") as String?
        false -> null
    }?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    /**
     * If all excluded or ignored tests should be run nevertheless.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val shouldUnexclude by lazy { project.properties.containsKey("unexclude") }

    /**
     * The properties of the project parsed into a [Config] object.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:Input
    val projectConfig: Config by lazy {
        ConfigValueFactory.fromMap(project.properties
                .filterValues { it is String || it is File }
                .mapValues { it.value.toString() })
                .toConfig()
    }

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:InputDirectory
    val sourcesPath by lazy { extension.sourcesPath }

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanPrivate")
    @get:OutputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path,
            SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    private val leafDirectoriesWithProperties by lazy {
        FilesUtils.getSortedLeafDirectories(sourcesPath)
                .map { it to resolveConfig(it) }
                .filter { (testPath, resolvedProperties) ->
                    when {
                        isTestExcluded(resolvedProperties) -> {
                            logger.warn("Excluding test ${testPath.cut(sourcesPath)}")

                            false
                        }
                        !isTestCoveredByTags(resolvedProperties) -> false
                        else -> true
                    }
                }
    }

    private val configCache = mutableMapOf<Path, Config>()
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

        leafDirectoriesWithProperties.forEach { (testPath, resolvedConfig) ->
            val requestPath = FilesUtils.validateExistence(testPath.resolve(REQUEST))
            val responsePath = FilesUtils.validateExistence(testPath.resolve(SOURCE_RESPONSE))
            val resolvedSqlScripts = resolveSqlScripts(testPath, resolvedConfig)

            val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(testPath.cut(sourcesPath)))
            val processedConfigPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(CONFIG))
            val processedRequestPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(REQUEST))
            val processedResponsePath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(EXPECTED_RESPONSE))

            val request = SAXReader().read(requestPath)
            val response = SAXReader().read(responsePath)

            resolvedConfig.preProcessors.forEach {
                val preProcessor = Class.forName(it).newInstance() as SquitPreProcessor

                preProcessor.process(request, response)
            }

            resolvedConfig.preProcessorScripts.forEach {
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

            resolvedConfig.writeTo(processedConfigPath)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun resolveConfig(testPath: Path): Config {
        var currentDirectoryPath = testPath
        var result = ConfigFactory.empty()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(Constants.CONFIG).also { configPath ->
                val newConfig = configCache.getOrPut(configPath, {
                    ConfigFactory.parseFile(configPath.toFile())
                })

                result = result.withFallback(newConfig).mergeTag(configPath.parent.fileName.toString())
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return try {
            result.resolveWith(projectConfig).validate()
        } catch (error: Throwable) {
            throw GradleException("Invalid test.conf file on path of test: ${testPath.cut(sourcesPath)}"
                    + " (${error.message})")
        }
    }

    private fun resolveSqlScripts(testPath: Path, config: Config): List<Pair<String, String>> {
        val result = mutableMapOf<String, String>()
        var currentDirectoryPath = testPath

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            val leafsFromHere = pathCache.getOrPut(currentDirectoryPath, {
                leafDirectoriesWithProperties
                        .filter { (path, _) -> path.startsWith(currentDirectoryPath) }
                        .map { (path, _) -> path }
            })

            config.databaseConfigurations.forEach { (name, _, _, _) ->
                val preName = "${name}_pre.sql"
                val postName = "${name}_post.sql"
                val preOnceName = "${name}_pre_once.sql"
                val postOnceName = "${name}_post_once.sql"

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

    private fun isTestExcluded(config: Config) = config.shouldExclude && !shouldUnexclude
    private fun isTestCoveredByTags(config: Config) = tags.isEmpty() || tags.any { it in config.tags }
}
