package de.smartsquare.squit

import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class containing the available extensions for the squit dsl.
 */
open class SquitExtension(project: Project) {

    /**
     * The jdbc driver class to use.
     */
    var jdbcDriver: String? = ""

    /**
     * The class for pre processing. If specified, it is expected to be on the classpath.
     */
    var preProcessClass: String? = ""

    /**
     * The class for post processing. If specified, it is expected to be on the classpath.
     */
    var postProcessClass: String? = ""

    /**
     * The path the sources lie in. Defaults to src/test.
     */
    var sourcesPath: Path? = Paths.get(project.projectDir.path, "src", "test")

    /**
     * The path to save reports and possible failures in.
     */
    var reportsPath: Path? = Paths.get(project.buildDir.path, "squit", "reports")
}