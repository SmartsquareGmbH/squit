package de.smartsquare.timrunner

import de.smartsquare.timrunner.task.TimRequestTask
import de.smartsquare.timrunner.task.TimTransformerTask
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
            project.tasks.create("requestTim", TimRequestTask::class.java) {
                it.group = "build"
                it.description = "Performs the requests specified in the test source directory."
            }

            project.tasks.create("transformTimResponses", TimTransformerTask::class.java) {
                it.group = "build"
                it.description = "Transforms the responses to be readable and usable for the comparing task."
                it.dependsOn("requestTim")
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
