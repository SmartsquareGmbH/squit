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
@Suppress("unused")
class SquitPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        try {
            val extension = project.extensions.create("squit", SquitExtension::class.java, project)

            project.tasks.create("squitPreProcess", SquitPreProcessTask::class.java) {
                it.extension = extension
            }

            project.tasks.create("squitRunRequests", SquitRequestTask::class.java) {
                it.extension = extension

                it.dependsOn("squitPreProcess")
            }

            project.tasks.create("squitPostProcess", SquitPostProcessTask::class.java) {
                it.extension = extension

                it.dependsOn("squitRunRequests")
            }

            project.tasks.create("squitTest", SquitTestTask::class.java) {
                it.extension = extension

                it.dependsOn("squitPostProcess")
            }
        } catch (error: MissingMethodException) {
            throw GradleException("Your Gradle version is too old.")
        }
    }
}
