package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.io.FilesUtils
import de.smartsquare.timrunner.logic.TimTransformer
import de.smartsquare.timrunner.util.Constants.CONFIG
import de.smartsquare.timrunner.util.Constants.REQUEST
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_POST
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_PRE
import de.smartsquare.timrunner.util.Constants.TIM_DB_POST
import de.smartsquare.timrunner.util.Constants.TIM_DB_PRE
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.read
import de.smartsquare.timrunner.util.safeStore
import de.smartsquare.timrunner.util.write
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Task for pre-processing the available sources like requests, responses, sql scripts and properties.
 */
open class TimPreProcessTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var sourcesPath: Path = Paths.get(project.projectDir.path, "src/main/test")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var processedSourcesPath: Path = Paths.get(project.buildDir.path, "source")

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
                val (requestPath, responsePath, sqlFilePaths) = getRelevantPathsForTest(it)
                val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(it.cut(sourcesPath)))

                val processedPropertiesPath = FilesUtils.createFileIfNotExists(processedResultPath.resolve(CONFIG))

                val processedRequestPath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(requestPath.fileName))
                val processedResponsePath = FilesUtils.createFileIfNotExists(processedResultPath
                        .resolve(responsePath.fileName))

                val request = SAXReader().read(requestPath)
                val response = SAXReader().read(responsePath)

                transform(request, response)

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
    private fun resolveProperties(testPath: Path): TimProperties {
        var currentDirectoryPath = testPath
        val result = TimProperties()

        while (!currentDirectoryPath.endsWith(sourcesPath.parent)) {
            currentDirectoryPath.resolve(CONFIG).also { propertiesPath ->
                if (Files.exists(propertiesPath)) {
                    result.fillFromProperties(propertiesPath)
                }
            }

            currentDirectoryPath = currentDirectoryPath.parent
        }

        return when (result.isValid()) {
            true -> result
            false -> throw GradleException("No config.properties file with the required properties on the path of " +
                    "test: ${testPath.cut(sourcesPath)}") // TODO: Better error
        }
    }

    /**
     * Finds and returns the relevant paths of the test located at the given [testPath].
     *
     * Relevant are the request and response, the config and the sql scripts.
     */
    private fun getRelevantPathsForTest(testPath: Path): Triple<Path, Path, List<Path>> {
        var requestPath: Path? = null
        var responsePath: Path? = null
        val sqlFilePaths = mutableListOf<Path>()

        Files.list(testPath).use {
            it.sequential().forEach { path ->
                when (path.fileName.toString()) {
                    REQUEST -> requestPath = path
                    RESPONSE -> responsePath = path
                    TIM_DB_PRE, TIM_DB_POST, TAXBASE_DB_PRE, TAXBASE_DB_POST -> sqlFilePaths.add(path)
                    CONFIG -> Unit
                    else -> logger.warn("Ignoring unknown file: ${path.fileName}")
                }
            }
        }

        requestPath?.let { safeRequestFile ->
            responsePath?.let { safeResponseFile ->
                return Triple(safeRequestFile, safeResponseFile, sqlFilePaths)
            }

            throw GradleException("Missing ${RESPONSE} for test: ${testPath.fileName}")
        }

        throw GradleException("Missing request.xml for test: ${testPath.fileName}")
    }

    /**
     * Transforms the given [request] and [expectedResponse] to properly match the actual responses later.
     */
    private fun transform(request: Document, expectedResponse: Document) {
        TimTransformer.replaceDateFromExpectedResponse(request, expectedResponse, "TaxAlgorithmDate")
        TimTransformer.replaceDateFromExpectedResponse(request, expectedResponse, "TaxCalculationDate")

        TimTransformer.sortTaxInvoiceSubTotals(expectedResponse, "SellerTaxTotal")
        TimTransformer.sortTaxInvoiceSubTotals(expectedResponse, "BuyerTaxTotal")

        TimTransformer.stripStackTraces(expectedResponse)
    }
}