package de.smartsquare.squit.config

import com.typesafe.config.Config
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.cut
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path

/**
 * Helper class for resolving the conf files.
 */
class ConfigResolver(
    private val configWalker: ConfigWalker,
    private val sourcesPath: Path,
    private val logger: Logger
) {

    /**
     * Walks the project structure and resolves the conf files with the leaf directories (containing the actual tests).
     * Filters the tests by the given [tags] and the exclude flag in the conf file, if [shouldUnexclude] is not set.
     */
    fun resolveWithLeafDirectories(tags: List<String>, shouldUnexclude: Boolean): List<Pair<Path, Config>> {
        return FilesUtils.getLeafDirectories(sourcesPath, sort = false)
            .filter { Files.newDirectoryStream(it).use { directories -> directories.any() } }
            .map { it to configWalker.walk(it) }
            .filter { (testPath, config) ->
                when {
                    isTestExcluded(config, shouldUnexclude) -> {
                        logger.info("Excluding test ${testPath.cut(sourcesPath)}")

                        false
                    }
                    !isTestCoveredByTags(config, tags) -> false
                    else -> true
                }
            }
            .map { (path, config) ->
                try {
                    path to config.resolve().validate()
                } catch (error: Throwable) {
                    throw GradleException(
                        "Invalid test.conf or local.conf file on path of test: ${path.cut(sourcesPath)}",
                        error
                    )
                }
            }
            .toList()
    }

    private fun isTestExcluded(config: Config, shouldUnexclude: Boolean): Boolean {
        return config.shouldExclude && !shouldUnexclude
    }

    private fun isTestCoveredByTags(config: Config, tags: List<String>): Boolean {
        return tags.isEmpty() || tags.any { it in config.tags }
    }
}
