package de.smartsquare.squit.interfaces

import com.typesafe.config.Config

/**
 * Interface for implementation of a squit pre-runner.
 */
interface SquitPreRunner {

    /**
     * Runs arbitrary code before a request execution. The [config] can be used to conveniently look up properties.
     */
    fun run(config: Config)
}
