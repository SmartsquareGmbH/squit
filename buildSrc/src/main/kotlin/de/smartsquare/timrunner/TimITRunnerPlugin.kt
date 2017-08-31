package de.smartsquare.timrunner

import groovy.lang.MissingMethodException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The main plugin class.
 *
 * @author Ruben Gees
 */
class TimITRunnerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        try {
            project.tasks.create("runTimITs", TimRequestTask::class.java) {
                it.group = "build"
                it.description = "Performs the requests specified in the test source directory."
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
