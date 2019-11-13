package de.smartsquare.squit.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.LOCAL_CONFIG
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper class for resolving [Config] objects from given [Path]s.
 */
class ConfigWalker(private val projectConfig: Config, private val sourcesPath: Path) {

    private val configCache = ConcurrentHashMap<Path, Config>()

    /**
     * Walks the configs found on the given [testPath] up to (including) the [sourcesPath].
     *
     * The configs are merged, starting with the config the most deep in the file tree.
     * In each directory, this method scans for a "local.conf" and "test.conf" file and the "local.conf" file
     * is merged before the "test.conf" file.
     */
    fun walk(testPath: Path): Config {
        val result = FilesUtils.walkUpwards(testPath) { it.endsWith(sourcesPath.parent) }
            .map { path -> path to resolve(path) }
            .fold(ConfigFactory.empty(), { acc: Config, next: Pair<Path, Config> ->
                val (path, config) = next

                acc.withFallback(config).mergeTag(path.fileName.toString())
            })

        return result.withFallback(projectConfig)
    }

    private fun resolve(path: Path): Config {
        val configPath = path.resolve(CONFIG)
        val localConfigPath = path.resolve(LOCAL_CONFIG)

        return configCache.getOrPut(path) {
            ConfigFactory.parseFile(localConfigPath.toFile())
                .withFallback(ConfigFactory.parseFile(configPath.toFile()))
        }
    }
}
