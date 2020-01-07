package de.smartsquare.squit

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.util.ConfigureUtil
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class containing the available extensions for the squit dsl.
 */
open class SquitExtension(project: Project) {

    /**
     * Extension for xml configuration.
     */
    @get:Nested
    val xml = XmlExtension()

    /**
     * Extension for json configuration.
     */
    @get:Nested
    val json = JsonExtension()

    /**
     * The jdbc driver classes to use.
     */
    @get:Input
    var jdbcDrivers: List<String> = emptyList()

    /**
     * The path the sources lie in. Defaults to src/squit.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    var sourcesPath: Path = Paths.get(project.projectDir.path, "src", "squit")

    /**
     * The path to save reports and possible failures in.
     */
    @get:OutputDirectory
    var reportsPath: Path = Paths.get(project.buildDir.path, "squit", "reports")

    /**
     * The timeout in seconds to use for requests.
     */
    @Suppress("MagicNumber")
    @get:Internal
    var timeout = 10L

    /**
     * If failures should be ignored.
     * In that case the task passes, even if tests have failed.
     */
    @get:Input
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
        @get:Input
        var strict = true

        /**
         * If the html report should be canonicalized for xml tests.
         */
        @get:Input
        var canonicalize = true
    }

    /**
     * Class containing the available extensions for the json dsl.
     */
    open class JsonExtension {

        /**
         * If the html report should be canonicalized for json tests.
         */
        @get:Input
        var canonicalize = true
    }
}
