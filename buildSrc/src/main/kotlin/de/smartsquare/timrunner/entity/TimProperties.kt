package de.smartsquare.timrunner.entity

import de.smartsquare.timrunner.util.safeLoad
import okhttp3.HttpUrl
import org.gradle.api.GradleException
import java.nio.file.Path
import java.util.*

class TimProperties {

    val endpoint get() = _endpoint ?: throw AssertionError("Internal representation is null.")
    val ignore get() = _ignore == true
    val timdbJdbc get() = _timdbJdbc ?: throw AssertionError("Internal representation is null.")
    val timdbUser get() = _timdbUser ?: throw AssertionError("Internal representation is null.")
    val timdbPassword get() = _timdbPassword ?: throw AssertionError("Internal representation is null.")
    val taxbasedbJdbc get() = _taxbasedbJdbc ?: throw AssertionError("Internal representation is null.")
    val taxbasedbUser get() = _taxbasedbUser ?: throw AssertionError("Internal representation is null.")
    val taxbasedbPassword
        get() = _taxbasedbPassword ?: throw AssertionError("Internal representation is null.")

    private var _endpoint: HttpUrl? = null
    private var _ignore: Boolean? = null
    private var _timdbJdbc: String? = null
    private var _taxbasedbJdbc: String? = null
    private var _timdbUser: String? = null
    private var _timdbPassword: String? = null
    private var _taxbasedbUser: String? = null
    private var _taxbasedbPassword: String? = null

    fun fillFromProperties(path: Path): TimProperties {
        Properties().safeLoad(path).also { properties ->
            if (_endpoint == null) {
                _endpoint = properties.getProperty("endpoint").let {
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

            if (_ignore == null) {
                _ignore = properties.getProperty("ignore").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        null -> null
                        else -> throw GradleException("Invalid value for ignore property: $it")
                    }
                }
            }

            if (_timdbJdbc == null) {
                _timdbJdbc = properties.getProperty("timdb_jdbc").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timdb_jdbc property: $it")
                        else -> it
                    }
                }
            }

            if (_timdbUser == null) {
                _timdbUser = properties.getProperty("timdb_user").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timdb_user property: $it")
                        else -> it
                    }
                }
            }

            if (_timdbPassword == null) {
                _timdbPassword = properties.getProperty("timdb_password").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timdb_password property: $it")
                        else -> it
                    }
                }
            }

            if (_taxbasedbJdbc == null) {
                _taxbasedbJdbc = properties.getProperty("taxbasedb_jdbc").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timstat_jdbc property: $it")
                        else -> it
                    }
                }
            }

            if (_taxbasedbUser == null) {
                _taxbasedbUser = properties.getProperty("taxbasedb_user").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timstat_user property: $it")
                        else -> it
                    }
                }
            }

            if (_taxbasedbPassword == null) {
                _taxbasedbPassword = properties.getProperty("taxbasedb_password").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for taxbasedb_password property: $it")
                        else -> it
                    }
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
        setProperty("timdb_jdbc", timdbJdbc)
        setProperty("timdb_user", timdbUser)
        setProperty("timdb_password", timdbPassword)
        setProperty("taxbasedb_jdbc", taxbasedbJdbc)
        setProperty("taxbasedb_user", taxbasedbUser)
        setProperty("taxbasedb_password", taxbasedbPassword)
    }

    fun isValid() = _endpoint != null && _timdbJdbc != null && _timdbUser != null && _timdbPassword != null
            && _taxbasedbJdbc != null && _taxbasedbUser != null && _taxbasedbPassword != null

    override fun toString() = "TimProperties(_endpoint=$_endpoint, _ignore=$_ignore, _timdbJdbc=$_timdbJdbc, " +
            "_taxbasedbJdbc=$_taxbasedbJdbc, _timdbUser=$_timdbUser, _timdbPassword=$_timdbPassword, " +
            "_taxbasedbUser=$_taxbasedbUser, _taxbasedbPassword=$_taxbasedbPassword)"
}
