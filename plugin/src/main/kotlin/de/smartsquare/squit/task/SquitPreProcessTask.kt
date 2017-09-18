package de.smartsquare.squit.task

import de.smartsquare.squit.SquitPluginExtension
import de.smartsquare.squit.SquitPreProcessor
import de.smartsquare.squit.entity.SquitProperties
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.CONFIG
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.Constants.REQUEST
import de.smartsquare.squit.util.Constants.SOURCE_RESPONSE
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.safeStore
import de.smartsquare.squit.util.write
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 *
 * @author Ruben Gees
 */
open class SquitPreProcessTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var sourcesPath: Path = Paths.get(project.projectDir.path, "src", "main", "test")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var processedSourcesPath: Path = Paths.get(project.buildDir.path, "sources")

    @Internal
    private val propertyCache = mutableMapOf<Path, SquitProperties>()

    @get:Internal
    private val processor by lazy {
        project.extensions.getByType(SquitPluginExtension::class.java)?.preProcessClass?.let {
            if (it.isNotBlank()) {
                logger.info("Using $it for pre processing.")

                Class.forName(it).newInstance() as SquitPreProcessor
            } else {
                null
            }
        }
    }

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(processedSourcesPath)
        Files.createDirectories(processedSourcesPath)

        FilesUtils.getSortedLeafDirectories(sourcesPath).forEach {
            val resolvedProperties = resolveProperties(it)

            if (!resolvedProperties.ignore) {
                val (requestPath, responsePath, sqlFilePaths) = getRelevantPathsForTest(it, resolvedProperties)
                val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(it.cut(sourcesPath)))

                val processedPropertiesPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(CONFIG))

                val processedRequestPath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(REQUEST))
                val processedResponsePath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(EXPECTED_RESPONSE))

                val request = SAXReader().read(requestPath)
                val response = SAXReader().read(responsePath)

                processor?.process(request, response)

                request.write(processedRequestPath)
                response.write(processedResponsePath)

                sqlFilePaths.forEach { Files.copy(it, processedResultPath.resolve(it.fileName), REPLACE_EXISTING) }

                resolvedProperties.writeToProperties().safeStore(processedPropertiesPath)
            } else {
                logger.warn("Ignoring test ${it.cut(sourcesPath)}")
            }
        }
    }

    /**
     * Recursively resolves properties starting at the bottommost directory. Already found attributes are not overridden
     * by later found ones which allows for setting attributes for many tests, while also being able to tweak for
     * individual ones.
     */
    private fun resolveProperties(testPath: Path): SquitProperties {
        var currentDirectoryPath = testPath
        val result = SquitProperties()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(CONFIG).also { propertiesPath ->
                if (Files.exists(propertiesPath)) {
                    val newProperties = propertyCache.getOrPut(propertiesPath, {
                        SquitProperties().fillFromProperties(propertiesPath, project.properties)
                    })

                    result.mergeWith(newProperties)
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return result.validateAndGetErrorMessage().let {
            when (it) {
                null -> result
                else -> throw GradleException("Invalid $CONFIG file on path of test: " +
                        "${testPath.cut(sourcesPath)} ($it)")
            }
        }
    }

    /**
     * Finds and returns the relevant paths of the test located at the given [testPath].
     *
     * Relevant are the request and response, the config and the sql scripts.
     */
    private fun getRelevantPathsForTest(testPath: Path, properties: SquitProperties): Triple<Path, Path, List<Path>> {
        var requestPath: Path? = null
        var responsePath: Path? = null
        val sqlFilePaths = mutableListOf<Path>()

        Files.list(testPath).use {
            it.sequential().forEach { path ->
                when (path.fileName.toString()) {
                    REQUEST -> requestPath = path
                    SOURCE_RESPONSE -> responsePath = path
                    CONFIG -> Unit
                    in properties.databaseConfigurations.map { "${it.name}_pre.sql" } -> sqlFilePaths.add(path)
                    in properties.databaseConfigurations.map { "${it.name}_post.sql" } -> sqlFilePaths.add(path)
                    else -> logger.warn("Ignoring unknown file ${path.fileName}")
                }
            }
        }

        requestPath?.let { safeRequestFile ->
            responsePath?.let { safeResponseFile ->
                return Triple(safeRequestFile, safeResponseFile, sqlFilePaths)
            }

            throw GradleException("Missing $SOURCE_RESPONSE for test: ${testPath.fileName}")
        }

        throw GradleException("Missing request.xml for test: ${testPath.fileName}")
    }
}