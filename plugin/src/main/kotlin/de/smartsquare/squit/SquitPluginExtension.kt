package de.smartsquare.squit

/**
 * Class containing the available extensions for the squit dsl.
 */
open class SquitPluginExtension {

    /**
     * The jdbc driver class to use.
     */
    var jdbcDriver: String = ""

    /**
     * The class for pre processing. If specified, it is expected to be on the classpath.
     */
    var preProcessClass: String = ""

    /**
     * The class for post processing. If specified, it is expected to be on the classpath.
     */
    var postProcessClass: String = ""
}