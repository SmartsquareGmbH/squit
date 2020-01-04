package de.smartsquare.squit.entity

import com.typesafe.config.Config
import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Entity describing a single indexed test for squit to run.
 *
 * @param path The path.
 * @param config The config.
 * @param request The path of the request.
 * @param response The path of the response.
 * @param preSqlScripts The map of sql scripts to run before the test.
 * @param postSqlScripts The map of sql scripts to run after the test.
 * @param descriptions The list of descriptions for the test.
 */
data class SquitTest(
    val path: Path,
    val config: Config,
    val request: Path?,
    val response: Path,
    val preSqlScripts: Map<String, List<Path>>,
    val postSqlScripts: Map<String, List<Path>>,
    val descriptions: List<Path>
) : Serializable {

    private companion object {
        private const val serialVersionUID = 1L
    }

    /**
     * Merges this with the [other] test.
     */
    fun merge(other: SquitTest): SquitTest {
        val mergedPreSqlScripts = preSqlScripts
            .mapValues { (key, scripts) -> (other.preSqlScripts[key] ?: emptyList()) + scripts }
            .plus(other.preSqlScripts.filterKeys { !preSqlScripts.containsKey(it) })

        val mergedPostSqlScripts = postSqlScripts
            .mapValues { (key, scripts) -> scripts + (other.postSqlScripts[key] ?: emptyList()) }
            .plus(other.postSqlScripts.filterKeys { !postSqlScripts.containsKey(it) })

        val mergedDescriptions = other.descriptions + descriptions

        return SquitTest(path, config, request, response, mergedPreSqlScripts, mergedPostSqlScripts, mergedDescriptions)
    }

    // Paths are not serializable so we have to copy to a special proxy class with Strings instead of paths.
    // This should be hidden from the user.
    @Suppress("UnusedPrivateMember")
    private fun writeReplace(): Any {
        return SquitTestSerializationProxy(
            path.toString(),
            config,
            request?.toString(),
            response.toString(),
            preSqlScripts.mapValues { (_, scripts) -> scripts.map { it.toString() } },
            postSqlScripts.mapValues { (_, scripts) -> scripts.map { it.toString() } },
            descriptions.map { it.toString() }
        )
    }

    private data class SquitTestSerializationProxy(
        val path: String,
        val config: Config,
        val request: String?,
        val response: String,
        val preSqlScripts: Map<String, List<String>>,
        val postSqlScripts: Map<String, List<String>>,
        val descriptions: List<String>
    ) : Serializable {

        private companion object {
            private const val serialVersionUID = 1L
        }

        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = SquitTest(
            Paths.get(path),
            config,
            request?.let { Paths.get(it) },
            Paths.get(response),
            preSqlScripts.mapValues { (_, scripts) -> scripts.map { Paths.get(it) } },
            postSqlScripts.mapValues { (_, scripts) -> scripts.map { Paths.get(it) } },
            descriptions.map { Paths.get(it) }
        )
    }
}
