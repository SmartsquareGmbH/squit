package de.smartsquare.squit.util

import groovy.lang.Binding
import groovy.lang.GroovyClassLoader
import groovy.lang.GroovyCodeSource
import groovy.lang.Script
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Object class for running groovy scripts with a cache for the classes.
 */
object GroovyScriptRunner {
    private val classLoader by lazy { GroovyClassLoader(javaClass.classLoader) }
    private val cache = ConcurrentHashMap<Path, Class<*>>()

    /**
     * Runs the script at [scriptPath] with the given [binding].
     */
    fun run(scriptPath: Path, binding: Binding) {
        val scriptClass = cache.computeIfAbsent(scriptPath) { path ->
            classLoader.parseClass(GroovyCodeSource(path.toFile()))
        }

        val script = (scriptClass.getDeclaredConstructor().newInstance() as Script)
        script.binding = binding
        script.run()
    }
}
