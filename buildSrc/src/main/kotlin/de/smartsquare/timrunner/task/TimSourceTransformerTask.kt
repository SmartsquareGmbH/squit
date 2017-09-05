package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.util.*
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
import java.nio.file.StandardCopyOption

open class TimSourceTransformerTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputSourceDirectory: Path = Paths.get(project.projectDir.path, "src/main/test")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory: Path = Paths.get(project.buildDir.path, "source")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.getLeafDirectories(inputSourceDirectory).forEach {
            val resolvedProperties = resolveProperties(it)

            if (!resolvedProperties.ignore) {
                val (requestPath, responsePath, sqlFilePaths) = getRelevantPathsForTest(it)
                val resultDirectory = Files.createDirectories(outputDirectory.resolve(it.cut(inputSourceDirectory)))

                val resultPropertiesPath = FilesUtils.createFileIfNotExists(resultDirectory
                        .resolve("config.properties"))

                val resultRequestPath = FilesUtils.createFileIfNotExists(resultDirectory
                        .resolve(requestPath.fileName))

                val resultResponsePath = FilesUtils.createFileIfNotExists(resultDirectory
                        .resolve(responsePath.fileName))

                transformRequest(SAXReader().read(requestPath)).write(resultRequestPath)
                transformResponse(SAXReader().read(responsePath)).write(resultResponsePath)

                sqlFilePaths.forEach {
                    Files.copy(it, resultDirectory.resolve(it.fileName), StandardCopyOption.REPLACE_EXISTING)
                }

                resolvedProperties
                        .writeToProperties()
                        .safeStore(resultPropertiesPath, "Properties for the ${it.fileName} test")
            }
        }
    }

    private fun resolveProperties(testDirectory: Path): TimProperties {
        var currentDirectory = testDirectory
        val result = TimProperties()

        while (!currentDirectory.endsWith(inputSourceDirectory)) {
            currentDirectory.resolve("config.properties").also { propertiesPath ->
                if (Files.exists(propertiesPath)) {
                    result.fillFromProperties(propertiesPath)
                }
            }

            currentDirectory = currentDirectory.parent
        }

        return when (result.isValid()) {
            true -> result
            false -> throw GradleException("No config.properties file with the required properties on the path of " +
                    "test: ${testDirectory.fileName}")
        }
    }

    private fun getRelevantPathsForTest(path: Path): Triple<Path, Path, List<Path>> {
        var requestPath: Path? = null
        var responsePath: Path? = null
        val sqlFilePaths = mutableListOf<Path>()

        Files.list(path).use {
            it.sequential().forEach { path ->
                when (path.fileName.toString()) {
                    "request.xml" -> requestPath = path
                    "response.xml" -> responsePath = path
                    "timdb_pre.sql", "timdb_post.sql", "timstat_pre.sql", "timstat_post.sql" -> sqlFilePaths.add(path)
                    "config.properties" -> Unit
                    else -> logger.warn("Ignoring unknown file: ${path.fileName}")
                }
            }
        }

        requestPath?.let { safeRequestFile ->
            responsePath?.let { safeResponseFile ->
                return Triple(safeRequestFile, safeResponseFile, sqlFilePaths)
            }

            throw GradleException("Missing response.xml for test: ${path.fileName}")
        }

        throw GradleException("Missing request.xml for test: ${path.fileName}")
    }

    private fun transformRequest(request: Document): Document {
        return request
    }

    private fun transformResponse(response: Document): Document {
        return response
    }
}
