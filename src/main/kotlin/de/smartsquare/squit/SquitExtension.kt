package de.smartsquare.squit

import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class containing the available extensions for the squit dsl.
 *
 * @author Ruben Gees
 */
open class SquitExtension(project: Project) {

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
}
