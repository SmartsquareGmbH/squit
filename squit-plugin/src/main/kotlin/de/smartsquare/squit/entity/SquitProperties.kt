package de.smartsquare.squit.entity

import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.PropertyUtils
import de.smartsquare.squit.util.getTemplateProperty
import de.smartsquare.squit.util.safeLoad
import nu.studer.java.util.OrderedProperties
import okhttp3.HttpUrl
import okhttp3.MediaType
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class for reading, holding and writing properties.
 *
 * @author Ruben Gees
 */
@Suppress("LargeClass", "TooManyFunctions", "")
class SquitProperties {

    companion object {

        /**
         * The endpoint property.
         */
        const val ENDPOINT_PROPERTY = "endpoint"

        /**
         * The mediaType property.
         */
        const val MEDIA_TYPE_PROPERTY = "mediaType"

        /**
         * The exclude property.
         */
        const val EXCLUDE_PROPERTY = "exclude"

        /**
         * The ignore property.
         */
        const val IGNORE_PROPERTY = "ignore"

        /**
         * The tags property.
         */
        const val TAGS_PROPERTY = "tags"

        /**
         * The preProcessors property.
         */
        const val PRE_PROCESSORS_PROPERTY = "preProcessors"

        /**
         * The preProcessorScripts property.
         */
        const val PRE_PROCESSOR_SCRIPTS_PROPERTY = "preProcessorScripts"

        /**
         * The postProcessors property.
         */
        const val POST_PROCESSORS_PROPERTY = "postProcessors"

        /**
         * The postProcessorScripts property.
         */
        const val POST_PROCESSOR_SCRIPTS_PROPERTY = "postProcessorScripts"

        private const val NULL_ERROR = "Internal representation is null."
    }

    /**
     * The endpoint.
     */
    val endpoint get() = internalEndpoint ?: throw AssertionError(NULL_ERROR)

    /**
     * The media type.
     */
    val mediaType get() = internalMediaType ?: throw AssertionError(NULL_ERROR)

    /**
     * If the test should be ignored.
     */
    val exclude get() = internalExclude == true

    /**
     * If the test should run, but not show up in the report.
     */
    val ignore get() = internalIgnore == true

    /**
     * Tags to select the tests to run.
     */
    val tags get() = internalTags

    /**
     * Class names of pre-processors to run.
     */
    val preProcessors get() = internalPreProcessors

    /**
     * Paths of pre-processor scripts to run.
     */
    val preProcessorScripts get() = internalPreProcessorScripts

    /**
     * Class names of post-processors to run.
     */
    val postProcessors get() = internalPostProcessors

    /**
     * Paths of post-processor scripts to run.
     */
    val postProcessorScripts get() = internalPostProcessorScripts

    /**
     * List of [SquitDatabaseConfiguration] objects to use.
     */
    val databaseConfigurations get() = internalDatabaseConfigurations.values

    private var internalEndpoint: HttpUrl? = null
    private var internalMediaType: MediaType? = null
    private var internalExclude: Boolean? = null
    private var internalIgnore: Boolean? = null
    private var internalTags: Set<String> = emptySet()
    private var internalPreProcessors: Set<String> = emptySet()
    private var internalPreProcessorScripts: Set<Path> = emptySet()
    private var internalPostProcessors: Set<String> = emptySet()
    private var internalPostProcessorScripts: Set<Path> = emptySet()
    private val internalDatabaseConfigurations = mutableMapOf<String, SquitDatabaseConfiguration>()

    /**
     * Fills this instance with the properties found at the given [path].
     *
     * The properties file is a usual [OrderedProperties] file and the values are required to be not blank.
     * This method is intended to be used multiple times for example in a recursive directory tree to gradually add all
     * required properties.
     */
    @Throws(GradleException::class)
    fun fillFromProperties(path: Path, projectProperties: Map<String, *>? = null): SquitProperties {
        if (Files.exists(path)) {
            PropertyUtils.newProperties().safeLoad(path).also { properties ->
                internalFillFromProperties(properties, projectProperties)
            }
        }

        path.parent.fileName?.let {
            internalTags += it.toString()
        }

        return this
    }

    /**
     * Fills this instance with the properties found at the given [path], but requires the file to contain all needed
     * properties.
     */
    @Throws(GradleException::class)
    fun fillFromSingleProperties(path: Path, projectProperties: Map<String, *>? = null) = this.apply {
        fillFromProperties(path, projectProperties)

        validateAndGetErrorMessage().let {
            when (it) {
                null -> this
                else -> throw GradleException("Invalid $CONFIG at path: $path ($it)")
            }
        }
    }

    /**
     * Merges this with another [SquitProperties] instance. Properties which already exist in this instance are not
     * changed.
     */
    fun mergeWith(other: SquitProperties) {
        if (internalEndpoint == null) internalEndpoint = other.internalEndpoint
        if (internalMediaType == null) internalMediaType = other.internalMediaType
        if (internalExclude == null) internalExclude = other.internalExclude
        if (internalIgnore == null) internalIgnore = other.internalIgnore

        internalTags += other.internalTags
        internalPreProcessors += other.internalPreProcessors
        internalPreProcessorScripts += other.internalPreProcessorScripts
        internalPostProcessors += other.internalPostProcessors
        internalPostProcessorScripts += other.internalPostProcessorScripts

        other.internalDatabaseConfigurations.forEach { (key, value) ->
            internalDatabaseConfigurations.putIfAbsent(key, value)
        }
    }

    /**
     * Converts this to an instance of [OrderedProperties].
     */
    fun writeToProperties() = PropertyUtils.newProperties().apply {
        setProperty(ENDPOINT_PROPERTY, endpoint.toString())
        setProperty(MEDIA_TYPE_PROPERTY, mediaType.toString())
        setProperty(EXCLUDE_PROPERTY, exclude.toString())
        setProperty(IGNORE_PROPERTY, ignore.toString())

        if (tags.isNotEmpty()) setProperty(TAGS_PROPERTY, tags.joinToString(","))

        if (preProcessors.isNotEmpty()) setProperty(PRE_PROCESSORS_PROPERTY,
                preProcessors.joinToString(","))

        if (preProcessorScripts.isNotEmpty()) setProperty(PRE_PROCESSOR_SCRIPTS_PROPERTY,
                preProcessorScripts.joinToString(","))

        if (postProcessors.isNotEmpty()) setProperty(POST_PROCESSORS_PROPERTY,
                postProcessors.joinToString(","))

        if (postProcessorScripts.isNotEmpty()) setProperty(POST_PROCESSOR_SCRIPTS_PROPERTY,
                postProcessorScripts.joinToString(","))

        internalDatabaseConfigurations.forEach { databaseName, (_, jdbcAddress, username, password) ->
            setProperty("db_${databaseName}_jdbc", jdbcAddress)
            setProperty("db_${databaseName}_username", username)
            setProperty("db_${databaseName}_password", password)
        }
    }

    /**
     * Validates if this instance contains all needed properties and returns a [String] with an error message if not.
     */
    fun validateAndGetErrorMessage() = when {
        internalEndpoint == null -> "endpoint property is missing"
        internalMediaType == null -> "mediaType property is missing"
        else -> null
    }

    private fun internalFillFromProperties(properties: OrderedProperties, projectProperties: Map<String, *>?) {
        if (internalEndpoint == null) {
            internalEndpoint = readAndValidateHttpUrlProperty(ENDPOINT_PROPERTY, properties, projectProperties)
        }

        if (internalMediaType == null) {
            internalMediaType = readAndValidateMediaTypeProperty(MEDIA_TYPE_PROPERTY, properties, projectProperties)
        }

        if (internalExclude == null) {
            internalExclude = readAndValidateBooleanProperty(EXCLUDE_PROPERTY, properties, projectProperties)
        }

        if (internalIgnore == null) {
            internalIgnore = readAndValidateBooleanProperty(IGNORE_PROPERTY,
                    properties, projectProperties)
        }

        internalTags += readAndValidateStringSetProperty(TAGS_PROPERTY, properties,
                projectProperties)
        internalPreProcessors += readAndValidateStringSetProperty(PRE_PROCESSORS_PROPERTY, properties,
                projectProperties)
        internalPreProcessorScripts += readAndValidatePathSetProperty(PRE_PROCESSOR_SCRIPTS_PROPERTY, properties,
                projectProperties)
        internalPostProcessors += readAndValidateStringSetProperty(POST_PROCESSORS_PROPERTY, properties,
                projectProperties)
        internalPostProcessorScripts += readAndValidatePathSetProperty(POST_PROCESSOR_SCRIPTS_PROPERTY, properties,
                projectProperties)

        readDatabaseConfigurations(properties, projectProperties)
    }

    private fun readDatabaseConfigurations(properties: OrderedProperties, projectProperties: Map<String, *>?) {
        properties
                .entrySet()
                .map { it.key.toString() }
                .filter { it.startsWith("db_") }
                .map { it.substringAfter("_").substringBeforeLast("_") }
                .distinct()
                .forEach { databaseName ->
                    if (!internalDatabaseConfigurations.containsKey(databaseName)) {
                        val jdbc = readAndValidateStringProperty("db_${databaseName}_jdbc", properties,
                                projectProperties)

                        val username = readAndValidateStringProperty("db_${databaseName}_username", properties,
                                projectProperties)

                        val password = readAndValidateStringProperty("db_${databaseName}_password", properties,
                                projectProperties)

                        if (jdbc == null || username == null || password == null) {
                            throw GradleException("A jdbc, username and password property is required in the same " +
                                    "properties file if a db property is declared.")
                        }

                        internalDatabaseConfigurations.put(databaseName,
                                SquitDatabaseConfiguration(databaseName, jdbc, username, password))
                    }
                }
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidateHttpUrlProperty(name: String, properties: OrderedProperties,
                                               projectProperties: Map<String, *>? = null): HttpUrl? {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            HttpUrl.parse(property).let { parsedProperty ->
                when (parsedProperty) {
                    null -> throwInvalidPropertyError(name, property)
                    else -> parsedProperty
                }
            }
        }
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidateBooleanProperty(name: String, properties: OrderedProperties,
                                               projectProperties: Map<String, *>? = null): Boolean? {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            when {
                property.equals("true", ignoreCase = true) -> true
                property.equals("false", ignoreCase = true) -> false
                else -> throwInvalidPropertyError(name, property)
            }
        }
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidateStringProperty(name: String, properties: OrderedProperties,
                                              projectProperties: Map<String, *>? = null): String? {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            when {
                property.isBlank() -> throwInvalidPropertyError(name, property)
                else -> property
            }
        }
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidateMediaTypeProperty(name: String, properties: OrderedProperties,
                                                 projectProperties: Map<String, *>? = null): MediaType? {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            MediaType.parse(property).let { parsedProperty ->
                when (parsedProperty) {
                    null -> throwInvalidPropertyError(name, property)
                    else -> parsedProperty
                }
            }
        }
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidateStringSetProperty(name: String, properties: OrderedProperties,
                                                 projectProperties: Map<String, *>? = null): Set<String> {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            when {
                property.isBlank() -> throwInvalidPropertyError(name, property)
                else -> property
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet()
            }
        } ?: emptySet()
    }

    @Suppress("ExpressionBodySyntax")
    private fun readAndValidatePathSetProperty(name: String, properties: OrderedProperties,
                                               projectProperties: Map<String, *>? = null): Set<Path> {
        return properties.getTemplateProperty(name, projectProperties)?.let { property ->
            when {
                property.isBlank() -> throwInvalidPropertyError(name, property)
                else -> property
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { Paths.get(it) }
                        .map { if (!Files.exists(it)) throwInvalidPropertyError(name, property) else it }
                        .toSet()
            }
        } ?: emptySet()
    }

    private fun throwInvalidPropertyError(name: String, property: String): Nothing {
        throw GradleException("Invalid value for $name property: $property")
    }
}
