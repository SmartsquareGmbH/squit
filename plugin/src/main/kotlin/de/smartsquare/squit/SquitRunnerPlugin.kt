package de.smartsquare.squit

import de.smartsquare.squit.task.SquitPostProcessTask
import de.smartsquare.squit.task.SquitPreProcessTask
import de.smartsquare.squit.task.SquitRequestTask
import de.smartsquare.squit.task.SquitTestTask
import groovy.lang.MissingMethodException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The main plugin class.
 *
 * @author Ruben Gees
 */
class SquitRunnerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        try {
            project.tasks.create("squitPreProcess", SquitPreProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the sources to be readable and usable for the following tasks."
            }

            project.tasks.create("squitRunRequests", SquitRequestTask::class.java) {
                it.group = "Build"
                it.description = "Performs the integration tests specified in the test source directory."
                it.dependsOn("squitPreProcess")
            }

            project.tasks.create("squitPostProgress", SquitPostProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the actual responses to be readable and usable for the comparing task."
                it.dependsOn("squitRunRequests")
            }

            project.tasks.create("squitTest", SquitTestTask::class.java) {
                it.group = "Build"
                it.description = "Compares the actual responses to the expected responses and generates a report."
                it.dependsOn("squitPostProgress")
            }

            project.extensions.add("squit", SquitPluginExtension::class.java)
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
