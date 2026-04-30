package de.smartsquare.squit.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.Constants.DESCRIPTION
import de.smartsquare.squit.util.permitsRequestBody
import de.smartsquare.squit.util.requiresRequestBody
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Class responsible for indexing the sources directory.
 *
 * @param projectConfig The project config used for resolving placeholders in individual test configs.
 */
class TestIndexer(private val projectConfig: Config) {

    private companion object {
        private val configExceptionMessageRegex = Regex("\\d: (.*)")
    }

    private val configCache = ConcurrentHashMap<Path, Config>()

    /**
     * Indexes the given [sourceDir] and returns a list of [SquitTest]s. The [filter] can be used to exclude
     * individual tests.
     */
    fun index(sourceDir: Path, filter: (Pair<Path, Config>) -> Boolean): List<SquitTest> {
        val leafDirectories = FilesUtils.getLeafDirectories(sourceDir, sort = true)
        val leafDirectoriesWithConfig = indexConfigs(leafDirectories, sourceDir, filter)

        return indexTests(leafDirectoriesWithConfig, sourceDir)
    }

    private fun indexConfigs(
        leafDirectories: Sequence<Path>,
        sourceDir: Path,
        filter: (Pair<Path, Config>) -> Boolean,
    ): List<Pair<Path, Config>> = leafDirectories
        .onEach { path ->
            if (sourceDir.relativize(path).toList().size < 2) {
                throw GradleException(
                    "Invalid project structure. Please add a project directory to the src/squit directory.",
                )
            }
        }
        .map { leafDirectory -> leafDirectory to resolveConfigs(leafDirectory, sourceDir) }
        .filter(filter)
        .map { (leafDirectory, config) ->
            try {
                leafDirectory to config.resolve().validate()
            } catch (error: Exception) {
                val innerMessage = when (error) {
                    is ConfigException ->
                        configExceptionMessageRegex
                            .find(error.message.orEmpty())
                            ?.groupValues?.getOrNull(1)

                    else -> error.message
                }

                throw GradleException(
                    """
                            |Invalid test.conf or local.conf file on path of test:
                            | ${sourceDir.relativize(leafDirectory)} ($innerMessage)
                    """.trimMargin().replace("\n", ""),
                    error,
                )
            }
        }
        .toList()

    private fun indexTests(leafDirectoriesWithConfig: List<Pair<Path, Config>>, sourceDir: Path): List<SquitTest> {
        val leavesByAncestor = mutableMapOf<Path, MutableList<Path>>()

        for ((leaf, _) in leafDirectoriesWithConfig) {
            FilesUtils.walkUpwards(leaf, sourceDir).forEach { ancestor ->
                leavesByAncestor.getOrPut(ancestor) { mutableListOf() }.add(leaf)
            }
        }

        return leafDirectoriesWithConfig
            .filterNot { (path, _) -> FilesUtils.isDirectoryEmpty(path) }
            .mapNotNull { (leafDirectory, config) ->
                val request = resolveRequest(leafDirectory, config)
                val response = resolveResponse(leafDirectory, config)

                val testParts = FilesUtils.walkUpwards(leafDirectory, sourceDir)
                    .map { path ->
                        val leafsFromHere = leavesByAncestor[path].orEmpty()

                        val sqlScripts = resolveSqlScripts(path, config, leafsFromHere, leafDirectory)

                        val preSqlScripts = sqlScripts.mapValues { (_, scripts) ->
                            listOfNotNull(scripts.preOnce, scripts.pre)
                        }

                        val postSqlScripts = sqlScripts.mapValues { (_, scripts) ->
                            listOfNotNull(scripts.post, scripts.postOnce)
                        }

                        val descriptions = listOfNotNull(resolveDescription(path))

                        SquitTest(path, config, request, response, preSqlScripts, postSqlScripts, descriptions)
                    }
                    .toList()

                when {
                    testParts.isEmpty() -> null
                    else -> testParts.reduce { acc, test -> acc.merge(test) }
                }
            }
    }

    private fun resolveConfigs(path: Path, sourceDir: Path): Config = FilesUtils.walkUpwards(path, sourceDir)
        .map { it to resolveConfig(it) }
        .fold(ConfigFactory.empty()) { acc, currentPathToConfig ->
            val (currentPath, config) = currentPathToConfig

            // Do not add tag for the last part since it is part of the sourceDir.
            if (currentPath.endsWith(sourceDir)) {
                acc.withFallback(config)
            } else {
                acc.withFallback(config).mergeTag(currentPath.fileName.toString())
            }
        }
        .let {
            projectConfig
                .withTestDir(path)
                .withFallback(it)
        }

    private fun resolveConfig(path: Path): Config {
        val configPath = path.resolve(Constants.CONFIG)
        val localConfigPath = path.resolve(Constants.LOCAL_CONFIG)

        return configCache.getOrPut(path) {
            ConfigFactory
                .parseFile(localConfigPath.toFile())
                .withFallback(ConfigFactory.parseFile(configPath.toFile()))
        }
    }

    private fun resolveRequest(path: Path, config: Config) = path
        .resolve(MediaTypeFactory.request(config.mediaType))
        .let {
            when {
                requiresRequestBody(config.method) -> FilesUtils.validateExistence(it)

                permitsRequestBody(config.method) -> when (Files.exists(it)) {
                    true -> it
                    else -> null
                }

                else -> null
            }
        }

    private fun resolveResponse(path: Path, config: Config): Path = FilesUtils.validateExistence(
        path.resolve(MediaTypeFactory.sourceResponse(config.mediaType)),
    )

    private fun resolveSqlScripts(
        path: Path,
        config: Config,
        leafs: List<Path>,
        leafPath: Path,
    ): Map<String, SqlScripts> = config.databaseConfigurations.associate { databaseConfig ->
        val pre = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_pre.sql"))
        val preOnce = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_pre_once.sql"))
            ?.takeIf { leafs.firstOrNull() == leafPath }

        val post = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_post.sql"))
        val postOnce = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_post_once.sql"))
            ?.takeIf { leafs.lastOrNull() == leafPath }

        databaseConfig.name to SqlScripts(pre, preOnce, post, postOnce)
    }

    private fun resolveDescription(path: Path): Path? = FilesUtils.ifExists(path.resolve(DESCRIPTION))

    private data class SqlScripts(val pre: Path?, val preOnce: Path?, val post: Path?, val postOnce: Path?)
}
