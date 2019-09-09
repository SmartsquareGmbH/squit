package de.smartsquare.squit

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class containing the available extensions for the squit dsl.
 *
 * @author Ruben Gees
 */
open class SquitExtension(project: Project) {

    val xml = XmlExtension()
    val json = JsonExtension()

    /**
     * The jdbc driver classes to use.
     */
    var jdbcDrivers: List<String> = emptyList()

    /**
     * The path the sources lie in. Defaults to src/test.
     */
    var sourcesPath: Path = Paths.get(project.projectDir.path, "src", "test")

    /**
     * The path to save reports and possible failures in.
     */
    var reportsPath: Path = Paths.get(project.buildDir.path, "squit", "reports")

    /**
     * The timeout in seconds to use for requests.
     */
    @Suppress("MagicNumber")
    var timeout = 10L

    /**
     * If failures should be ignored.
     * In that case the task passes, even if tests have failed.
     */
    var ignoreFailures = false

    /**
     * Configures the xml dsl.
     */
    fun xml(closure: Closure<*>) {
        ConfigureUtil.configure(closure, xml)
    }

    /**
     * Configures the xml dsl.
     */
    fun xml(action: Action<XmlExtension>) {
        action.execute(xml)
    }

    /**
     * Class containing the available extensions for the xml dsl.
     */
    open class XmlExtension {

        /**
         * If the xml diffing should use strict (e.g. identic) comparison.
         */
        var strict = true

        /**
         * If the html report should be canonicalized for xml tests.
         */
        var canonicalize = true
    }

    open class JsonExtension {

        /**
         * If the html report should be canonicalized for json tests.
         */
        var canonicalize = true
    }
}
