package de.smartsquare.squit

import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.task.SquitPostProcessTask
import de.smartsquare.squit.task.SquitPreProcessTask
import de.smartsquare.squit.task.SquitRequestTask
import de.smartsquare.squit.task.SquitTestTask
import de.smartsquare.squit.util.asPath
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

/**
 * The main plugin class.
 */
@Suppress("unused")
class SquitPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (GradleVersion.current() < GradleVersion.version("6.8")) {
            throw GradleException(
                "Minimum supported Gradle version is 6.8. Current version is ${GradleVersion.current().version}.",
            )
        }

        val extension = project.extensions.create("squit", SquitExtension::class.java, project)

        project.tasks.register("squitPreProcess", SquitPreProcessTask::class.java) {
            it.sourceDir = extension.sourceDir.asPath
        }

        project.tasks.register("squitRunRequests", SquitRequestTask::class.java) {
            it.jdbcDrivers = extension.jdbcDrivers
            it.timeout = extension.timeout
            it.silent = extension.silent

            it.dependsOn("squitPreProcess")
            it.outputs.upToDateWhen { false }
        }

        project.tasks.register("squitPostProcess", SquitPostProcessTask::class.java) {
            it.dependsOn("squitRunRequests")
        }

        project.tasks.register("squitTest", SquitTestTask::class.java) {
            it.reportDir = extension.reportDir.asPath
            it.silent = extension.silent
            it.ignoreFailures = extension.ignoreFailures
            it.mediaTypeConfig = MediaTypeConfig(
                extension.xml.strict,
                extension.xml.canonicalize,
                extension.xml.resolveInvalidNamespaces,
                extension.json.canonicalize,
            )

            it.dependsOn("squitPostProcess")
        }
    }
}
