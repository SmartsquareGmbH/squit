package de.smartsquare.timrunner

import de.smartsquare.timrunner.task.TimITTask
import de.smartsquare.timrunner.task.TimRequestTask
import de.smartsquare.timrunner.task.TimResponseTransformerTask
import groovy.lang.MissingMethodException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

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

            project.tasks.create("transformTimResponses", TimResponseTransformerTask::class.java) {
                it.group = "build"
                it.description = "Transforms the responses to be readable and usable for the comparing task."
                it.dependsOn("requestTim")
            }

            project.tasks.create("runTimITs", TimITTask::class.java) {
                it.group = "build"
                it.description = "Runs the integration tests."
                it.dependsOn("transformTimResponses")

                it.reports.getXml().apply {
                    isEnabled = true
                    destination = File(project.buildDir, "reports/timIT")
                }

                it.reports.getHtml().apply {
                    isEnabled = false
                    destination = File(project.buildDir, "reports/timIT")
                }
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
