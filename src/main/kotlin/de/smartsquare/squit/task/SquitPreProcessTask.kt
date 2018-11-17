package de.smartsquare.squit.task

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.DESCRIPTION
import de.smartsquare.squit.util.Constants.ERROR
import de.smartsquare.squit.util.Constants.SOURCES_DIRECTORY
import de.smartsquare.squit.util.Constants.SQUIT_DIRECTORY
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.databaseConfigurations
import de.smartsquare.squit.util.mediaType
import de.smartsquare.squit.util.mergeTag
import de.smartsquare.squit.util.method
import de.smartsquare.squit.util.shouldExclude
import de.smartsquare.squit.util.tags
import de.smartsquare.squit.util.validate
import de.smartsquare.squit.util.writeTo
import okhttp3.internal.http.HttpMethod
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
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Input
    val tags = when (project.hasProperty("tags")) {
        true -> project.property("tags") as String?
        false -> null
    }?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    /**
     * If all excluded or ignored tests should be run nevertheless.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Input
    val shouldUnexclude by lazy { project.properties.containsKey("unexclude") }

    /**
     * The properties of the project parsed into a [Config] object.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Input
    val projectConfig: Config by lazy {
        ConfigValueFactory.fromMap(project.properties
            .filterKeys { it is String && it.startsWith("squit.") }
            .mapKeys { (key, _) -> key.replaceFirst("squit.", "") })
            .toConfig()
    }

    /**
     * The directory of the test sources.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:InputDirectory
    val sourcesPath by lazy { extension.sourcesPath }

    /**
     * The directory to save the results in.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputDirectory
    val processedSourcesPath: Path = Paths.get(project.buildDir.path, SQUIT_DIRECTORY, SOURCES_DIRECTORY)

    @get:Internal
    internal var extension by Delegates.notNull<SquitExtension>()

    private val leafDirectoriesWithConfig by lazy {
        FilesUtils.getSortedLeafDirectories(sourcesPath)
            .filter { Files.newDirectoryStream(it).use { directories -> directories.any() } }
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

        leafDirectoriesWithConfig.forEach { (testPath, resolvedConfig) ->
            if (testPath.cut(sourcesPath).toList().size < 2) {
                throw GradleException(
                    "Invalid project structure. Please add a project directory to the src/test directory."
                )
            }

            val requestPath = resolveRequestPath(resolvedConfig, testPath)

            val responsePath = FilesUtils.validateExistence(
                testPath.resolve(MediaTypeFactory.sourceResponse(resolvedConfig.mediaType))
            )

            val resolvedSqlScripts = resolveSqlScripts(testPath, resolvedConfig)
            val resolvedDescriptions = resolveDescriptions(testPath)

            val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(testPath.cut(sourcesPath)))
            val processedConfigPath = processedResultPath.resolve(CONFIG)

            val processedRequestPath = processedResultPath
                .resolve(MediaTypeFactory.request(resolvedConfig.mediaType))

            val processedResponsePath = processedResultPath
                .resolve(MediaTypeFactory.expectedResponse(resolvedConfig.mediaType))

            try {
                MediaTypeFactory.processor(resolvedConfig.mediaType)
                    .preProcess(requestPath, responsePath, processedRequestPath, processedResponsePath, resolvedConfig)
            } catch (error: Throwable) {
                Files.write(processedResultPath.resolve(ERROR), error.toString().toByteArray())
            }

            resolvedSqlScripts.forEach { (name, content) ->
                Files.write(processedResultPath.resolve(name), content.toByteArray())
            }

            if (resolvedDescriptions.isNotEmpty()) {
                resolvedDescriptions.joinToString(separator = "\n\n", postfix = "\n") { it.trim() }
                    .also { joinedDescription ->
                        Files.write(processedResultPath.resolve(DESCRIPTION), joinedDescription.toByteArray())
                    }
            }

            resolvedConfig.writeTo(processedConfigPath)
        }
    }

    @Suppress("SwallowedException")
    private fun resolveConfig(testPath: Path): Config {
        var currentDirectoryPath = testPath
        var result = ConfigFactory.empty()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(CONFIG).also { configPath ->
                val newConfig = configCache.getOrPut(configPath) {
                    ConfigFactory.parseFile(configPath.toFile())
                }

                result = result.withFallback(newConfig).mergeTag(configPath.parent.fileName.toString())
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return try {
            projectConfig.withFallback(result).resolve().validate()
        } catch (error: Throwable) {
            throw GradleException(
                "Invalid test.conf file on path of test: ${testPath.cut(sourcesPath)} (${error.message})"
            )
        }
    }

    private fun resolveRequestPath(config: Config, testPath: Path) = testPath
        .resolve(MediaTypeFactory.request(config.mediaType))
        .let {
            when {
                HttpMethod.requiresRequestBody(config.method) -> FilesUtils.validateExistence(it)
                HttpMethod.permitsRequestBody(config.method) -> when (Files.exists(it)) {
                    true -> it
                    else -> null
                }
                else -> null
            }
        }

    private fun resolveSqlScripts(testPath: Path, config: Config): List<Pair<String, String>> {
        val result = mutableMapOf<String, String>()
        var currentDirectoryPath = testPath

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            val leafsFromHere = pathCache.getOrPut(currentDirectoryPath) {
                leafDirectoriesWithConfig
                    .filter { (path, _) -> path.startsWith(currentDirectoryPath) }
                    .map { (path, _) -> path }
            }

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

                    result[preName] = content + result.getOrDefault(preName, "")
                }

                if (Files.exists(postPath)) {
                    val content = Files.readAllBytes(postPath).toString(Charsets.UTF_8)

                    result[postName] = result.getOrDefault(postName, "") + content
                }

                if (Files.exists(preOncePath) && leafsFromHere.indexOf(testPath) == 0) {
                    val content = Files.readAllBytes(preOncePath).toString(Charsets.UTF_8)

                    result[preName] = content + result.getOrDefault(preName, "")
                }

                if (Files.exists(postOncePath) && leafsFromHere.indexOf(testPath) == leafsFromHere.lastIndex) {
                    val content = Files.readAllBytes(postOncePath).toString(Charsets.UTF_8)

                    result[postName] = result.getOrDefault(postName, "") + content
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.toList()
    }

    private fun resolveDescriptions(testPath: Path): List<String> {
        val result = mutableListOf<String>()
        var currentDirectoryPath = testPath

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(DESCRIPTION).also { descriptionPath ->
                if (Files.exists(descriptionPath)) {
                    result.add(0, Files.readAllBytes(descriptionPath).toString(Charsets.UTF_8))
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.filter { it.isNotBlank() }
    }

    private fun isTestExcluded(config: Config) = config.shouldExclude && !shouldUnexclude
    private fun isTestCoveredByTags(config: Config) = tags.isEmpty() || tags.any { it in config.tags }
}
