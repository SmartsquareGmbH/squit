package de.smartsquare.squit

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

open class SquitExtension @Inject constructor(private val project: Project) {

    /**
     * Extension for xml configuration.
     */
    @Nested
    val xml: XmlExtension = project.objects.newInstance(XmlExtension::class.java, project)

    /**
     * Extension for json configuration.
     */
    @Nested
    val json: JsonExtension = project.objects.newInstance(JsonExtension::class.java, project)

    /**
     * The jdbc driver classes to use.
     */
    val jdbcDrivers: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * The path the sources lie in. Defaults to src/squit.
     */
    val sourceDir: DirectoryProperty = project.objects.directoryProperty()

    /**
     * The path to save reports and possible failures in.
     */
    val reportDir: DirectoryProperty = project.objects.directoryProperty()

    /**
     * The timeout in seconds to use for requests.
     */
    val requestTimeout: Property<Long> = project.objects.property(Long::class.java)

    /**
     * If squit should avoid printing anything if all tests pass.
     */
    val silent: Property<Boolean> = project.objects.property(Boolean::class.java)

    /**
     * If failures should be ignored.
     * In that case the task passes, even if tests have failed.
     */
    val ignoreFailures: Property<Boolean> = project.objects.property(Boolean::class.java)

    /**
     * Helper method to set the sourceDir via String.
     */
    fun sourceDir(path: String) = sourceDir.set(project.layout.projectDirectory.dir(path))

    /**
     * Helper method to set the reportDir via String.
     */
    fun reportDir(path: String) = reportDir.set(project.layout.projectDirectory.dir(path))

    /**
     * Configures the xml dsl.
     */
    fun xml(action: Action<XmlExtension>) {
        action.execute(xml)
    }

    /**
     * Configures the json dsl.
     */
    fun json(action: Action<JsonExtension>) {
        action.execute(json)
    }

    /**
     * Class containing the available extensions for the xml dsl.
     */
    open class XmlExtension @Inject constructor(project: Project) {

        /**
         * If the xml diffing should use strict (e.g. identic) comparison.
         */
        val strict: Property<Boolean> = project.objects.property(Boolean::class.java)

        /**
         * If the html report should be canonicalized for xml tests.
         */
        val canonicalize: Property<Boolean> = project.objects.property(Boolean::class.java)

        /**
         * Whether to try to resolve invalid namespaces on canonicalization (e.g. missing http://)
         */
        val resolveInvalidNamespaces: Property<Boolean> = project.objects.property(Boolean::class.java)
    }

    /**
     * Class containing the available extensions for the json dsl.
     */
    open class JsonExtension @Inject constructor(project: Project) {

        /**
         * If the html report should be canonicalized for json tests.
         */
        val canonicalize: Property<Boolean> = project.objects.property(Boolean::class.java)
    }
}
