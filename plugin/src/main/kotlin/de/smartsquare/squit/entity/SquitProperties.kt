package de.smartsquare.squit.entity

import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Utils
import de.smartsquare.squit.util.getTemplateProperty
import de.smartsquare.squit.util.safeLoad
import nu.studer.java.util.OrderedProperties
import okhttp3.HttpUrl
import org.gradle.api.GradleException
import java.nio.file.Path
import java.util.*

/**
 * Class for reading, holding and writing tim related properties.
 *
 * @author Ruben Gees
 */
class SquitProperties {

    private companion object {
        private const val ENDPOINT_PROPERTY = "endpoint"
        private const val IGNORE_PROPERTY = "ignore"
        private const val IGNORE_FOR_REPORT_PROPERTY = "ignoreForReport"
    }

    /**
     * The endpoint.
     */
    val endpoint get() = internalEndpoint ?: throw AssertionError("Internal representation is null.")

    /**
     * If the test should be ignored.
     */
    val ignore get() = internalIgnore == true

    /**
     * If the test should run, but not show up in the report.
     */
    val ignoreForReport get() = internalIgnoreForReport == true

    /**
     * List of [SquitDatabaseConfiguration] objects to use.
     */
    val databaseConfigurations get() = internalDatabaseConfigurations.values

    private var internalEndpoint: HttpUrl? = null
    private var internalIgnore: Boolean? = null
    private var internalIgnoreForReport: Boolean? = null
    private val internalDatabaseConfigurations = mutableMapOf<String, SquitDatabaseConfiguration>()

    /**
     * Fills this instance with the properties found at the given [path].
     *
     * The properties file is a usual [Properties] file and the values are required to be not blank.
     * This method is intended to be used multiple times for example in a recursive directory tree to gradually add all
     * required properties.
     */
    @Throws(GradleException::class)
    fun fillFromProperties(path: Path, projectProperties: Map<String, *>? = null): SquitProperties {
        Utils.newProperties().safeLoad(path).also { properties ->
            if (internalEndpoint == null) {
                internalEndpoint = readAndValidateHttpUrlProperty(ENDPOINT_PROPERTY, properties, projectProperties)
            }

            if (internalIgnore == null) {
                internalIgnore = readAndValidateBooleanProperty(IGNORE_PROPERTY, properties, projectProperties)
            }

            if (internalIgnoreForReport == null) {
                internalIgnoreForReport = readAndValidateBooleanProperty(IGNORE_FOR_REPORT_PROPERTY, properties,
                        projectProperties)
            }

            readDatabaseConfigurations(properties, projectProperties)
        }

        return this
    }

    /**
     * Fills this instance with the properties found at the given [path], but requires the file to contain all needed
     * properties.
     */
    @Throws(GradleException::class)
    fun fillFromSingleProperties(path: Path): SquitProperties {
        return this.apply {
            fillFromProperties(path)

            validateAndGetErrorMessage().let {
                when (it) {
                    null -> this
                    else -> throw GradleException("Invalid $CONFIG at path: $path ($it)")
                }
            }
        }
    }

    /**
     * Merges this with another [SquitProperties] instance. Properties which already exist in this instance are not
     * changed.
     */
    fun mergeWith(other: SquitProperties) {
        if (internalEndpoint == null) internalEndpoint = other.internalEndpoint
        if (internalIgnore == null) internalIgnore = other.internalIgnore
        if (internalIgnoreForReport == null) internalIgnoreForReport = other.internalIgnoreForReport

        other.internalDatabaseConfigurations.forEach { (key, value) ->
            internalDatabaseConfigurations.putIfAbsent(key, value)
        }
    }

    /**
     * Converts this to an instance of [Properties].
     */
    fun writeToProperties() = Utils.newProperties().apply {
        setProperty(ENDPOINT_PROPERTY, endpoint.toString())
        setProperty(IGNORE_PROPERTY, ignore.toString())
        setProperty(IGNORE_FOR_REPORT_PROPERTY, ignoreForReport.toString())

        internalDatabaseConfigurations.forEach { databaseName, (_, jdbcAddress, username, password) ->
            setProperty("db_${databaseName}_jdbc", jdbcAddress)
            setProperty("db_${databaseName}_username", username)
            setProperty("db_${databaseName}_password", password)
        }
    }

    /**
     * Validates if this instance contains all needed properties and returns a [String] with an error message if not.
     */
    fun validateAndGetErrorMessage(): String? {
        return when (internalEndpoint != null) {
            true -> null
            false -> "endpoint property is missing"
        }
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
                            throw GradleException("A jdbc, username and password property is required in the same" +
                                    "properties file if a db property is declared.")
                        }

                        internalDatabaseConfigurations.put(databaseName,
                                SquitDatabaseConfiguration(databaseName, jdbc, username, password))
                    }
                }
    }

    private fun readAndValidateHttpUrlProperty(name: String, properties: OrderedProperties,
                                               projectProperties: Map<String, *>? = null): HttpUrl? {
        return properties.getTemplateProperty(name, projectProperties)?.let {
            when {
                it.isBlank() -> throw GradleException("Invalid value for $name property: $it")
                else -> HttpUrl.parse(it).let {
                    when (it) {
                        null -> throw GradleException("Invalid value for $name property: $it")
                        else -> it
                    }
                }
            }
        }
    }

    private fun readAndValidateBooleanProperty(name: String, properties: OrderedProperties,
                                               projectProperties: Map<String, *>? = null): Boolean? {
        return properties.getTemplateProperty(name, projectProperties)?.let {
            when {
                it.equals("true", ignoreCase = true) -> true
                it.equals("false", ignoreCase = true) -> false
                else -> throw GradleException("Invalid value for $name property: $it")
            }
        }
    }

    private fun readAndValidateStringProperty(name: String, properties: OrderedProperties,
                                              projectProperties: Map<String, *>? = null): String? {
        return properties.getTemplateProperty(name, projectProperties)?.let {
            when {
                it.isBlank() -> throw GradleException("Invalid value for $name property: $it")
                else -> it
            }
        }
    }
}
