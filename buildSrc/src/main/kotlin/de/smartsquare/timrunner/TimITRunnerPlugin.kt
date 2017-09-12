package de.smartsquare.timrunner

import de.smartsquare.timrunner.task.*
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
            project.tasks.create("transformTimSources", TimPreProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the sources to be readable and usable for the following tasks."
            }

            project.tasks.create("requestTim", TimRequestTask::class.java) {
                it.group = "Build"
                it.description = "Performs the requests specified in the test source directory."
                it.dependsOn("transformTimSources")
            }

            project.tasks.create("transformTimResponses", TimPostProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the responses to be readable and usable for the comparing task."
                it.dependsOn("requestTim")
            }

            project.tasks.create("runTimITs", TimITTask::class.java) {
                it.group = "Build"
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

            project.tasks.create("convertSupplyChainProject", TimSupplyChainConverterTask::class.java) {
                it.group = "Build Setup"
                it.description = "Converts a legacy supply chain project to be usable by the ${project.name}."
                it.outputs.upToDateWhen { false }
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
