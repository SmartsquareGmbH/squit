package de.smartsquare.squit.task

import com.typesafe.config.Config
import de.smartsquare.squit.config.databaseConfigurations
import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.config.method
import de.smartsquare.squit.config.writeTo
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import okhttp3.internal.http.HttpMethod
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Worker processing each file of the [SquitPreProcessTask].
 */
@Suppress("UnstableApiUsage")
abstract class SquitPreProcessWorker : WorkAction<SquitPreProcessWorker.PreProcessParameters> {

    private val sourcesPath get() = parameters.sourcesPath.asPath
    private val processedSourcesPath get() = parameters.processedSourcesPath.asPath

    private val testPath get() = parameters.testPath.asPath
    private val resolvedConfig get() = parameters.resolvedConfig.get()

    private val pathCache = ConcurrentHashMap<Path, List<Path>>()

    override fun execute() {
        val requestPath = resolveRequestPath(resolvedConfig, testPath)

        val responsePath = FilesUtils.validateExistence(
            testPath.resolve(MediaTypeFactory.sourceResponse(resolvedConfig.mediaType))
        )

        val resolvedSqlScripts = resolveSqlScripts(testPath, resolvedConfig)
        val resolvedDescriptions = resolveDescriptions(testPath)

        val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(testPath.cut(sourcesPath)))
        val processedConfigPath = processedResultPath.resolve(Constants.CONFIG)

        val processedRequestPath = processedResultPath
            .resolve(MediaTypeFactory.request(resolvedConfig.mediaType))

        val processedResponsePath = processedResultPath
            .resolve(MediaTypeFactory.expectedResponse(resolvedConfig.mediaType))

        try {
            MediaTypeFactory.processor(resolvedConfig.mediaType)
                .preProcess(requestPath, responsePath, processedRequestPath, processedResponsePath, resolvedConfig)
        } catch (error: Throwable) {
            Files.write(processedResultPath.resolve(Constants.ERROR), error.toString().toByteArray())
        }

        resolvedSqlScripts.forEach { (name, content) ->
            Files.write(processedResultPath.resolve(name), content.toByteArray())
        }

        if (resolvedDescriptions.isNotEmpty()) {
            resolvedDescriptions.joinToString(separator = "\n\n", postfix = "\n") { it.trim() }
                .also { joinedDescription ->
                    Files.write(processedResultPath.resolve(Constants.DESCRIPTION), joinedDescription.toByteArray())
                }
        }

        resolvedConfig.writeTo(processedConfigPath)
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

    private fun resolveSqlScripts(
        testPath: Path,
        config: Config
    ): List<Pair<String, String>> {
        val result = mutableMapOf<String, String>()
        var currentDirectoryPath = testPath

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            val allLeafs = FilesUtils.getLeafDirectories(sourcesPath)
            val leafsFromHere = pathCache.getOrPut(currentDirectoryPath) {
                allLeafs.filter { path -> path.startsWith(currentDirectoryPath) }.toList()
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
            currentDirectoryPath.resolve(Constants.DESCRIPTION).also { descriptionPath ->
                if (Files.exists(descriptionPath)) {
                    result.add(0, Files.readAllBytes(descriptionPath).toString(Charsets.UTF_8))
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.filter { it.isNotBlank() }
    }

    interface PreProcessParameters : WorkParameters {
        val sourcesPath: DirectoryProperty
        val processedSourcesPath: DirectoryProperty
        val testPath: DirectoryProperty
        val resolvedConfig: Property<Config>
    }
}
