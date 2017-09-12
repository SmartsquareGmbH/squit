package de.smartsquare.timrunner

import de.smartsquare.timrunner.task.*
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
            project.tasks.create("preProcessTimSources", TimPreProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the sources to be readable and usable for the following tasks."
            }

            project.tasks.create("runTimRequests", TimRequestTask::class.java) {
                it.group = "Build"
                it.description = "Performs the integration tests specified in the test source directory."
                it.dependsOn("preProcessTimSources")
            }

            project.tasks.create("postProcessTimSources", TimPostProcessTask::class.java) {
                it.group = "Build"
                it.description = "Transforms the actual responses to be readable and usable for the comparing task."
                it.dependsOn("runTimRequests")
            }

            project.tasks.create("testTim", TimTestTask::class.java) {
                it.group = "Build"
                it.description = "Compares the actual responses to the expected responses and generates a report."
                it.dependsOn("postProcessTimSources")
            }

            project.tasks.create("convertSupplyChainProject", TimSupplyChainConverterTask::class.java) {
                it.group = "Build Setup"
                it.description = "Converts a legacy supply-chain project to be usable by the ${project.name}."
                it.outputs.upToDateWhen { false }
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
