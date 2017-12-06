@file:Suppress("NOTHING_TO_INLINE")

package de.smartsquare.squit.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.entity.SquitDatabaseConfiguration
import de.smartsquare.squit.io.FilesUtils
import okhttp3.HttpUrl
import okhttp3.MediaType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val ENDPOINT = "endpoint"
private const val MEDIA_TYPE = "mediaType"
private const val MEDIA_TYPE_FALLBACK = "application/xml"
private const val EXCLUDE = "exclude"
private const val IGNORE = "ignore"
private const val PRE_PROCESSORS = "preProcessors"
private const val PRE_PROCESSOR_SCRIPTS = "preProcessorScripts"
private const val POST_PROCESSORS = "postProcessors"
private const val POST_PROCESSOR_SCRIPTS = "postProcessorScripts"
private const val TAGS = "tags"
private const val DATABASE_CONFIGURATIONS = "databaseConfigurations"
private const val DATABASE_CONFIGURATION_NAME = "name"
private const val DATABASE_CONFIGURATION_JDBC_ADDRESS = "jdbc"
private const val DATABASE_CONFIGURATION_USERNAME = "username"
private const val DATABASE_CONFIGURATION_PASSWORD = "password"

/**
 * The endpoint to request against.
 */
val Config.endpoint: HttpUrl
    get() = getString(ENDPOINT).let { endpoint ->
        HttpUrl.parse(endpoint) ?: throw IllegalStateException("Invalid $ENDPOINT: $endpoint")
    }

/**
 * The mediaType to use for the request. If none is given, application/xml is used as fallback.
 */
val Config.mediaType
    get() = getSafeString(MEDIA_TYPE, MEDIA_TYPE_FALLBACK).let { mediaType ->
        MediaType.parse(mediaType) ?: throw IllegalStateException("Invalid $MEDIA_TYPE: $mediaType")
    }

/**
 * If the test should be excluded. Exclusion means that the test is not run at all.
 */
val Config.shouldExclude get() = getSafeBoolean(EXCLUDE)

/**
 * If the test should be ignored. Ignoring means that the test is run but not reported.
 */
val Config.shouldIgnore get() = getSafeBoolean(IGNORE)

/**
 * List of pre-processors to use.
 */
val Config.preProcessors get() = getSafeStringList(PRE_PROCESSORS)

/**
 * List of paths to pre-processor scripts to use.
 */
val Config.preProcessorScripts get() = getSafePathList(PRE_PROCESSOR_SCRIPTS)

/**
 * List of post-processors to use.
 */
val Config.postProcessors get() = getSafeStringList(POST_PROCESSORS)

/**
 * List of paths to post-processor scripts to use.
 */
val Config.postProcessorScripts get() = getSafePathList(POST_PROCESSOR_SCRIPTS)

/**
 * List of tags associated with the test.
 */
val Config.tags get() = getSafeStringList(TAGS)

/**
 * List of [SquitDatabaseConfiguration] objects.
 */
val Config.databaseConfigurations
    get() = getSafeConfigList(DATABASE_CONFIGURATIONS).map {
        SquitDatabaseConfiguration(
                it.getString(DATABASE_CONFIGURATION_NAME),
                it.getString(DATABASE_CONFIGURATION_JDBC_ADDRESS),
                it.getString(DATABASE_CONFIGURATION_USERNAME),
                it.getString(DATABASE_CONFIGURATION_PASSWORD)
        )
    }

/**
 * Merges the given [tag] into the existing List of tags or creates a new one with it.
 */
fun Config.mergeTag(tag: String) = withValue(TAGS, ConfigValueFactory.fromIterable(listOf(tag).plus(this.tags)))

/**
 * Validates all properties of this instance and throws if a problem is detected.
 */
@Suppress("ComplexMethod", "ThrowsCount")
fun Config.validate() = this.apply {
    // Call getters of properties to check existence and correct declaration.
    endpoint; mediaType; shouldExclude; shouldIgnore

    preProcessors.forEach { Class.forName(it) }
    preProcessorScripts.forEach { FilesUtils.validateExistence(it) }
    postProcessors.forEach { Class.forName(it) }
    postProcessorScripts.forEach { FilesUtils.validateExistence(it) }
    tags.forEach { if (it.isEmpty()) throw IllegalStateException("tags cannot be empty.") }
    databaseConfigurations.forEach { (name, jdbcAddress, username, password) ->
        if (name.isEmpty()) throw IllegalStateException("name of a databaseConfiguration cannot be empty.")
        if (jdbcAddress.isEmpty()) throw IllegalStateException("jdbc of a databaseConfiguration cannot be empty.")
        if (username.isEmpty()) throw IllegalStateException("username of a databaseConfiguration cannot be empty.")
        if (password.isEmpty()) throw IllegalStateException("password of a databaseConfiguration cannot be empty.")
    }
}

/**
 * Writes this config to the given [path] and applies the given [options] when rendering.
 */
inline fun Config.writeTo(
        path: Path,
        options: ConfigRenderOptions = ConfigRenderOptions.defaults()
                .setComments(false)
                .setOriginComments(false)
                .setJson(false)
) = Files.write(path, root().render(options).toByteArray())

private inline fun Config.getSafeBoolean(path: String, fallback: Boolean = false) = when (hasPath(path)) {
    true -> getBoolean(path)
    false -> fallback
}

private inline fun Config.getSafeString(path: String, fallback: String = "") = when (hasPath(path)) {
    true -> getString(path)
    false -> fallback
}

private inline fun Config.getSafeStringList(
        path: String,
        fallback: List<String> = emptyList()
): List<String> = when (hasPath(path)) {
    true -> getStringList(path)
    false -> fallback
}

private inline fun Config.getSafeConfigList(path: String, fallback: List<Config> = emptyList()) = when (hasPath(path)) {
    true -> getConfigList(path)
    false -> fallback
}

private inline fun Config.getSafePathList(path: String, fallback: List<Path> = emptyList()) = when (hasPath(path)) {
    true -> getStringList(path).map { Paths.get(it) }
    false -> fallback
}
