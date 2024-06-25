package de.smartsquare.squit

import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.task.SquitPostProcessTask
import de.smartsquare.squit.task.SquitPreProcessTask
import de.smartsquare.squit.task.SquitRequestTask
import de.smartsquare.squit.task.SquitTestTask
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
        if (GradleVersion.current() < GradleVersion.version("7.3")) {
            throw GradleException(
                "Minimum supported Gradle version is 7.3. Current version is ${GradleVersion.current().version}.",
            )
        }

        val extension = project.extensions.create("squit", SquitExtension::class.java, project).apply {
            jdbcDrivers.convention(emptyList())
            sourceDir.convention(project.layout.projectDirectory.dir("src/squit"))
            reportDir.convention(project.layout.buildDirectory.dir("squit/reports"))
            requestTimeout.convention(10)
            silent.convention(false)
            ignoreFailures.convention(false)
            xml.strict.convention(true)
            xml.canonicalize.convention(true)
            xml.resolveInvalidNamespaces.convention(false)
            json.canonicalize.convention(true)
        }

        project.tasks.register("squitPreProcess", SquitPreProcessTask::class.java) {
            it.sourceDir.set(extension.sourceDir)
        }

        project.tasks.register("squitRunRequests", SquitRequestTask::class.java) {
            it.jdbcDrivers.set(extension.jdbcDrivers)
            it.requestTimeout.set(extension.requestTimeout)
            it.silent.set(extension.silent)

            it.dependsOn("squitPreProcess")
            it.outputs.upToDateWhen { false }
        }

        project.tasks.register("squitPostProcess", SquitPostProcessTask::class.java) {
            it.dependsOn("squitRunRequests")
        }

        project.tasks.register("squitTest", SquitTestTask::class.java) {
            it.reportDir.set(extension.reportDir)
            it.silent.set(extension.silent)
            it.ignoreFailures.set(extension.ignoreFailures)

            it.mediaTypeConfig.set(
                MediaTypeConfig(
                    xmlStrict = extension.xml.strict.get(),
                    xmlCanonicalize = extension.xml.canonicalize.get(),
                    xmlResolveInvalidNamespaces = extension.xml.resolveInvalidNamespaces.get(),
                    jsonCanonicalize = extension.json.canonicalize.get(),
                ),
            )

            it.dependsOn("squitPostProcess")
        }
    }
}
