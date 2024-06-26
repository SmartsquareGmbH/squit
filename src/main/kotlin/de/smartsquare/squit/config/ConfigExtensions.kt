@file:Suppress("TooManyFunctions")

package de.smartsquare.squit.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import de.smartsquare.squit.entity.SquitDatabaseConfiguration
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.task.SquitPostTestTask
import de.smartsquare.squit.task.SquitPreTestTask
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val TITLE = "title"
private const val TEST_DIRECTORY = "testDir"
private const val ENDPOINT = "endpoint"
private const val MEDIA_TYPE = "mediaType"
private const val MEDIA_TYPE_FALLBACK = "text/plain"
private const val METHOD = "method"
private const val METHOD_FALLBACK = "POST"
private const val EXCLUDE = "exclude"
private const val IGNORE = "ignore"
private const val PRE_PROCESSORS = "preProcessors"
private const val PRE_PROCESSOR_SCRIPTS = "preProcessorScripts"
private const val PRE_RUNNERS = "preRunners"
private const val PRE_RUN_SCRIPTS = "preRunnerScripts"
private const val PRE_TEST_TASKS = "preTestTasks"
private const val POST_PROCESSORS = "postProcessors"
private const val POST_PROCESSOR_SCRIPTS = "postProcessorScripts"
private const val POST_RUNNERS = "postRunners"
private const val POST_RUN_SCRIPTS = "postRunnerScripts"
private const val POST_TEST_TASKS = "postTestTasks"
private const val TAGS = "tags"
private const val DATABASE_CONFIGURATIONS = "databaseConfigurations"
private const val DATABASE_CONFIGURATION_NAME = "name"
private const val DATABASE_CONFIGURATION_JDBC_ADDRESS = "jdbc"
private const val DATABASE_CONFIGURATION_USERNAME = "username"
private const val DATABASE_CONFIGURATION_PASSWORD = "password"
private const val HEADERS = "headers"
private const val EXPECTED_RESPONSE_CODE = "expectedResponseCode"

/**
 * The alternative title of the test.
 */
val Config.title: String get() = getSafeString(TITLE)

/**
 * The path to the directory of the test
 */
val Config.testDir: Path get() = FilesUtils.validateExistence(Paths.get(getSafeString(TEST_DIRECTORY)))

/**
 * The endpoint to request against.
 */
val Config.endpoint: HttpUrl
    get() = getString(ENDPOINT).let { endpoint ->
        endpoint.toHttpUrlOrNull() ?: error("Invalid $ENDPOINT: $endpoint")
    }

/**
 * The mediaType to use for the request. If none is given, text/plain is used as fallback.
 */
val Config.mediaType
    get() = getSafeString(MEDIA_TYPE, MEDIA_TYPE_FALLBACK).let { mediaType ->
        mediaType.toMediaTypeOrNull() ?: error("Invalid $MEDIA_TYPE: $mediaType")
    }

/**
 * The method to use for the request. This also decides if a request.xml is required or not. If none is given,
 * POST is used as fallback.
 */
val Config.method: String get() = getSafeString(METHOD, METHOD_FALLBACK)

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
 * List of pre-runners to use.
 */
val Config.preRunners get() = getSafeStringList(PRE_RUNNERS)

/**
 * List of paths to pre-run scripts to use.
 */
val Config.preRunnerScripts get() = getSafePathList(PRE_RUN_SCRIPTS)

/**
 * preTestTasks to execute.
 * default: PRE_RUNNERS, PRE_RUNNER_SCRIPTS, DATABASE_SCRIPTS
 */
val Config.preTestTasks: List<SquitPreTestTask>
    get() = when (hasPath(PRE_TEST_TASKS)) {
        true -> getEnumList(SquitPreTestTask::class.java, PRE_TEST_TASKS)
        else -> listOf(
            SquitPreTestTask.PRE_RUNNERS,
            SquitPreTestTask.PRE_RUNNER_SCRIPTS,
            SquitPreTestTask.DATABASE_SCRIPTS
        )
    }

/**
 * List of post-processors to use.
 */
val Config.postProcessors get() = getSafeStringList(POST_PROCESSORS)

/**
 * List of paths to post-processor scripts to use.
 */
val Config.postProcessorScripts get() = getSafePathList(POST_PROCESSOR_SCRIPTS)

/**
 * List of post-runners to use.
 */
val Config.postRunners get() = getSafeStringList(POST_RUNNERS)

/**
 * List of paths to pre-run scripts to use.
 */
val Config.postRunnerScripts get() = getSafePathList(POST_RUN_SCRIPTS)

/**
 * postTestTasks to execute.
 * default: DATABASE_SCRIPTS, POST_RUNNERS, POST_RUNNER_SCRIPTS
 */
val Config.postTestTasks: List<SquitPostTestTask>
    get() = when (hasPath(POST_TEST_TASKS)) {
        true -> getEnumList(SquitPostTestTask::class.java, POST_TEST_TASKS)
        else -> listOf(
            SquitPostTestTask.DATABASE_SCRIPTS,
            SquitPostTestTask.POST_RUNNERS,
            SquitPostTestTask.POST_RUNNER_SCRIPTS
        )
    }

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
 * List of headers to use for the request.
 */
val Config.headers
    get() = getSafeConfig(HEADERS)
        .entrySet()
        .map { it.toPair() }
        .associate { (key, value) -> key to value.unwrapped().toString() }

/**
 * The http status response code that is expected in the response.
 */
val Config.expectedResponseCode get() = getSafeInt(EXPECTED_RESPONSE_CODE)

/**
 * Merges the given [tag] into the existing List of tags or creates a new one with it.
 */
fun Config.mergeTag(tag: String): Config = withValue(TAGS, ConfigValueFactory.fromIterable(this.tags.plus(tag)))

/**
 * Sets the given [testDir].
 */
fun Config.withTestDir(testDir: Path): Config = withValue(
    TEST_DIRECTORY,
    ConfigValueFactory.fromAnyRef(testDir.toString())
)

/**
 * Validates all properties of this instance and throws if a problem is detected.
 */
fun Config.validate() = this.apply {
    // Call getters of properties to check existence and correct declaration.
    endpoint; mediaType; shouldExclude; shouldIgnore; headers; testDir; preTestTasks; postTestTasks

    preProcessors.forEach { checkClass(it) }
    preProcessorScripts.forEach { FilesUtils.validateExistence(it) }
    preRunnerScripts.forEach { FilesUtils.validateExistence(it) }
    preRunners.forEach { checkClass(it) }
    postProcessors.forEach { checkClass(it) }
    postProcessorScripts.forEach { FilesUtils.validateExistence(it) }
    postRunnerScripts.forEach { FilesUtils.validateExistence(it) }
    postRunners.forEach { checkClass(it) }
    tags.forEach { require(it.isNotEmpty()) { "tags cannot be empty." } }
    databaseConfigurations.forEach {
        require(it.name.isNotEmpty()) { "name of a databaseConfiguration cannot be empty." }
        require(it.jdbcAddress.isNotEmpty()) { "jdbc of a databaseConfiguration cannot be empty." }
        require(it.username.isNotEmpty()) { "username of a databaseConfiguration cannot be empty." }
        require(it.password.isNotEmpty()) { "password of a databaseConfiguration cannot be empty." }
    }
    require(expectedResponseCode == 0 || expectedResponseCode in 100..599) {
        "expectedResponseCode not in HTTP status code range."
    }
}

/**
 * Writes this config to the given [path] and applies the given [options] when rendering.
 */
fun Config.writeTo(
    path: Path,
    options: ConfigRenderOptions = ConfigRenderOptions.defaults()
        .setComments(false)
        .setOriginComments(false)
        .setJson(false)
): Path = Files.write(path, root().render(options).toByteArray())

private fun Config.getSafeBoolean(path: String, fallback: Boolean = false) = when (hasPath(path)) {
    true -> getBoolean(path)
    false -> fallback
}

private fun Config.getSafeString(path: String, fallback: String = "") = when (hasPath(path)) {
    true -> getString(path)
    false -> fallback
}

private fun Config.getSafeStringList(
    path: String,
    fallback: List<String> = emptyList()
): List<String> = when (hasPath(path)) {
    true -> getStringList(path)
    false -> fallback
}

private fun Config.getSafeConfig(path: String, fallback: Config = ConfigFactory.empty()) = when (hasPath(path)) {
    true -> getConfig(path)
    false -> fallback
}

private fun Config.getSafeConfigList(path: String, fallback: List<Config> = emptyList()) = when (hasPath(path)) {
    true -> getConfigList(path)
    false -> fallback
}

private fun Config.getSafePathList(path: String, fallback: List<Path> = emptyList()) = when (hasPath(path)) {
    true -> getStringList(path).map { Paths.get(it) }
    false -> fallback
}

private fun Config.getSafeInt(path: String, fallback: Int = 0) = when (hasPath(path)) {
    true -> getInt(path)
    false -> fallback
}

private fun checkClass(name: String) {
    try {
        Class.forName(name)
    } catch (error: ClassNotFoundException) {
        throw GradleException("Missing class: $name", error)
    } catch (error: Exception) {
        throw GradleException("Could not load class: $name", error)
    }
}
