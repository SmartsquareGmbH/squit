package de.smartsquare.squit.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.Constants.DESCRIPTION
import de.smartsquare.squit.util.cut
import okhttp3.internal.http.HttpMethod
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

    private val configCache = ConcurrentHashMap<Path, Config>()
    private val leafCache = ConcurrentHashMap<Path, List<Path>>()

    /**
     * Indexes the given [sourcesPath] and returns a list of [SquitTest]s. The [filter] can be used to exclude
     * individual tests.
     */
    fun index(sourcesPath: Path, filter: (Pair<Path, Config>) -> Boolean): List<SquitTest> {
        val leafDirectories = FilesUtils.getLeafDirectories(sourcesPath, sort = true).asSequence()
        val leafDirectoriesWithConfig = indexConfigs(leafDirectories, sourcesPath, filter)

        return indexTests(leafDirectoriesWithConfig, sourcesPath)
    }

    private fun indexConfigs(
        leafDirectories: Sequence<Path>,
        sourcesPath: Path,
        filter: (Pair<Path, Config>) -> Boolean
    ): List<Pair<Path, Config>> {
        return leafDirectories
            .onEach { path ->
                if (path.cut(sourcesPath).toList().size < 2) {
                    throw GradleException(
                        "Invalid project structure. Please add a project directory to the src/test directory."
                    )
                }
            }
            .map { leafDirectory -> leafDirectory to resolveConfigs(leafDirectory, sourcesPath) }
            .filter(filter)
            .map { (leafDirectory, config) ->
                try {
                    leafDirectory to config.resolve().validate()
                } catch (error: Throwable) {
                    throw GradleException(
                        "Invalid test.conf or local.conf file on path of test: ${leafDirectory.cut(sourcesPath)}",
                        error
                    )
                }
            }
            .toList()
    }

    private fun indexTests(
        leafDirectoriesWithConfig: List<Pair<Path, Config>>,
        sourcesPath: Path
    ): List<SquitTest> {
        return leafDirectoriesWithConfig
            .mapNotNull { (leafDirectory, config) ->
                val request = resolveRequest(leafDirectory, config)
                val response = resolveResponse(leafDirectory, config)

                val testParts = FilesUtils.walkUpwards(leafDirectory, sourcesPath.parent)
                    .map { path ->
                        val leafsFromHere = leafCache.getOrPut(path) {
                            leafDirectoriesWithConfig
                                .filter { (leaf, _) -> leaf.startsWith(path) }
                                .map { (leaf, _) -> leaf }
                                .toList()
                        }

                        val sqlScripts = resolveSqlScripts(path, config, leafsFromHere, leafDirectory)

                        val preSqlScripts = sqlScripts.mapValues { (_, scripts) ->
                            (scripts.preOnce?.let { listOf(it) } ?: emptyList()) +
                                (scripts.pre?.let { listOf(it) } ?: emptyList())
                        }

                        val postSqlScripts = sqlScripts.mapValues { (_, scripts) ->
                            (scripts.post?.let { listOf(it) } ?: emptyList()) +
                                (scripts.postOnce?.let { listOf(it) } ?: emptyList())
                        }

                        val descriptions = resolveDescription(path)?.let { listOf(it) } ?: emptyList()

                        SquitTest(path, config, request, response, preSqlScripts, postSqlScripts, descriptions)
                    }
                    .toList()

                when {
                    testParts.isEmpty() -> null
                    else -> testParts.reduce { acc, test -> acc.merge(test) }
                }
            }
    }

    private fun resolveConfigs(path: Path, sourcesPath: Path): Config {
        return FilesUtils.walkUpwards(path, sourcesPath.parent)
            .map { resolveConfig(it) }
            .fold(ConfigFactory.empty()) { acc, config ->
                acc.withFallback(config).mergeTag(path.fileName.toString())
            }
            .let { projectConfig.withFallback(it) }
    }

    private fun resolveConfig(path: Path): Config {
        val configPath = path.resolve(Constants.CONFIG)
        val localConfigPath = path.resolve(Constants.LOCAL_CONFIG)

        return configCache.getOrPut(path) {
            ConfigFactory.parseFile(localConfigPath.toFile())
                .withFallback(ConfigFactory.parseFile(configPath.toFile()))
        }
    }

    private fun resolveRequest(path: Path, config: Config) = path
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

    private fun resolveResponse(path: Path, config: Config): Path {
        return FilesUtils.validateExistence(
            path.resolve(MediaTypeFactory.sourceResponse(config.mediaType))
        )
    }

    private fun resolveSqlScripts(
        path: Path,
        config: Config,
        leafs: List<Path>,
        leafPath: Path
    ): Map<String, SqlScripts> {
        return config.databaseConfigurations
            .map { databaseConfig ->
                val pre = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_pre.sql"))
                val preOnce = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_pre_once.sql"))
                    ?.takeIf { leafs.firstOrNull() == leafPath }

                val post = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_post.sql"))
                val postOnce = FilesUtils.ifExists(path.resolve("${databaseConfig.name}_post_once.sql"))
                    ?.takeIf { leafs.lastOrNull() == leafPath }

                databaseConfig.name to SqlScripts(pre, preOnce, post, postOnce)
            }
            .toMap()
    }

    private fun resolveDescription(path: Path): Path? {
        return FilesUtils.ifExists(path.resolve(DESCRIPTION))
    }

    private data class SqlScripts(val pre: Path?, val preOnce: Path?, val post: Path?, val postOnce: Path?)
}