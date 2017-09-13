package de.smartsquare.timrunner.entity

import de.smartsquare.timrunner.util.safeLoad
import okhttp3.HttpUrl
import org.gradle.api.GradleException
import java.nio.file.Path
import java.util.*

class TimProperties {

    val endpoint get() = internalEndpoint ?: throw AssertionError("Internal representation is null.")
    val ignore get() = internalIgnore == true
    val ignoreForReport get() = internalIgnoreForReport == true
    val databaseConfigurations get() = internalDatabaseConfigurations.values

    private var internalEndpoint: HttpUrl? = null
    private var internalIgnore: Boolean? = null
    private var internalIgnoreForReport: Boolean? = null
    private val internalDatabaseConfigurations = mutableMapOf<String, TimDatabaseConfiguration>()

    fun fillFromProperties(path: Path): TimProperties {
        Properties().safeLoad(path).also { properties ->
            if (internalEndpoint == null) {
                internalEndpoint = properties.getProperty("endpoint").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for endpoint property: $it")
                        else -> HttpUrl.parse(it).let {
                            when (it) {
                                null -> throw GradleException("Invalid value for endpoint property: $it")
                                else -> it
                            }
                        }
                    }
                }
            }

            if (internalIgnore == null) {
                internalIgnore = properties.getProperty("ignore").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        null -> null
                        else -> throw GradleException("Invalid value for ignore property: $it")
                    }
                }
            }

            if (internalIgnoreForReport == null) {
                internalIgnoreForReport = properties.getProperty("ignoreForReport").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        null -> null
                        else -> throw GradleException("Invalid value for ignoreForReport property: $it")
                    }
                }
            }

            properties
                    .map { it.key.toString() }
                    .filter { it.startsWith("db_") }
                    .map { it.substringAfter("_").substringBeforeLast("_") }
                    .distinct()
                    .forEach { databaseName ->
                        if (!internalDatabaseConfigurations.containsKey(databaseName)) {
                            val jdbc = properties.getProperty("db_${databaseName}_jdbc")?.let {
                                when {
                                    it.isBlank() -> throw GradleException("Invalid value for property " +
                                            "db_${databaseName}_jdbc: $it")
                                    else -> it
                                }
                            }

                            val username = properties.getProperty("db_${databaseName}_username")?.let {
                                when {
                                    it.isBlank() -> throw GradleException("Invalid value for property " +
                                            "db_${databaseName}_username: $it")
                                    else -> it
                                }
                            }

                            val password = properties.getProperty("db_${databaseName}_password")?.let {
                                when {
                                    it.isBlank() -> throw GradleException("Invalid value for property " +
                                            "db_${databaseName}_password: $it")
                                    else -> it
                                }
                            }

                            if (jdbc == null || username == null || password == null) {
                                throw GradleException("A jdbc, username and password property is required in the same" +
                                        "properties file if a db property is declared.")
                            }

                            internalDatabaseConfigurations.put(databaseName,
                                    TimDatabaseConfiguration(databaseName, jdbc, username, password))
                        }
                    }
        }

        return this
    }

    fun fillFromSingleProperties(path: Path): TimProperties {
        fillFromProperties(path)

        return when (isValid()) {
            true -> this
            false -> throw IllegalArgumentException("Invalid properties at path: $path")
        }
    }

    fun writeToProperties() = Properties().apply {
        setProperty("endpoint", endpoint.toString())
        setProperty("ignore", ignore.toString())
        setProperty("ignoreForReport", ignoreForReport.toString())

        internalDatabaseConfigurations.forEach { databaseName, (_, jdbcAddress, username, password) ->
            setProperty("db_${databaseName}_jdbc", jdbcAddress)
            setProperty("db_${databaseName}_username", username)
            setProperty("db_${databaseName}_password", password)
        }
    }

    fun isValid() = internalEndpoint != null
}
