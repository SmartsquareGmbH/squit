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
    val timstatJdbc get() = _timstatJdbc ?: throw AssertionError("Internal representation is null.")
    val timstatUser get() = _timstatUser ?: throw AssertionError("Internal representation is null.")
    val timstatPassword get() = _timstatPassword ?: throw AssertionError("Internal representation is null.")

    private var _endpoint: HttpUrl? = null
    private var _ignore: Boolean? = null
    private var _timdbJdbc: String? = null
    private var _timstatJdbc: String? = null
    private var _timdbUser: String? = null
    private var _timdbPassword: String? = null
    private var _timstatUser: String? = null
    private var _timstatPassword: String? = null

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

            if (_timstatJdbc == null) {
                _timstatJdbc = properties.getProperty("timstat_jdbc").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timstat_jdbc property: $it")
                        else -> it
                    }
                }
            }

            if (_timstatUser == null) {
                _timstatUser = properties.getProperty("timstat_user").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timstat_user property: $it")
                        else -> it
                    }
                }
            }

            if (_timstatPassword == null) {
                _timstatPassword = properties.getProperty("timstat_password").let {
                    when {
                        it == null -> null
                        it.isBlank() -> throw GradleException("Invalid value for timstat_password property: $it")
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
        setProperty("ignore", ignore.toString())
        setProperty("timdb_jdbc", timdbJdbc)
        setProperty("timdb_user", timdbUser)
        setProperty("timdb_password", timdbPassword)
        setProperty("timstat_jdbc", timstatJdbc)
        setProperty("timstat_user", timstatUser)
        setProperty("timstat_password", timstatPassword)
    }

    fun isValid() = _endpoint != null && _timdbJdbc != null && _timdbUser != null && _timdbPassword != null
            && _timstatJdbc != null && _timstatUser != null && _timstatPassword != null

    override fun toString(): String {
        return "TimProperties(_endpoint=$_endpoint, _ignore=${_ignore == true}, _timdbJdbc=$_timdbJdbc, " +
                "_timstatJdbc=$_timstatJdbc, _timdbUser=$_timdbUser, _timdbPassword=$_timdbPassword, " +
                "_timstatUser=$_timstatUser, _timstatPassword=$_timstatPassword)"
    }
}
