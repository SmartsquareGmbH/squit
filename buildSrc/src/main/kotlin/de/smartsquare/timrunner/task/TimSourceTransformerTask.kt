package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.entity.TimProperties
import de.smartsquare.timrunner.util.TimOutputFormat
import de.smartsquare.timrunner.util.use
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

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
        getLeafDirectories(inputSourceDirectory).forEach {
            val (requestPath, responsePath, sqlFilePaths) = getRelevantPathsForTest(it)

            val resultDirectory = Files.createDirectories(outputDirectory.resolve(it.subtract(inputSourceDirectory)
                    .reduce { current, path -> current.resolve(path) }))

            val resultPropertiesPath = resultDirectory.resolve("config.properties").let { propertiesPath ->
                when (Files.exists(propertiesPath)) {
                    true -> propertiesPath
                    false -> Files.createFile(propertiesPath)
                }
            }

            val resultRequestPath = resultDirectory.resolve(requestPath.fileName)
            val resultResponsePath = resultDirectory.resolve(responsePath.fileName)

            val resolvedProperties = resolveProperties(it)

            if (Files.notExists(resultRequestPath)) Files.createFile(resultRequestPath)
            if (Files.notExists(resultResponsePath)) Files.createFile(resultResponsePath)

            SAXReader().read(requestPath.toFile()).let { requestDocument ->
                XMLWriter(Files.newBufferedWriter(resultRequestPath), TimOutputFormat()).use {
                    it.write(requestDocument)
                }
            }

            SAXReader().read(responsePath.toFile()).let { responseDocument ->
                XMLWriter(Files.newBufferedWriter(resultResponsePath), TimOutputFormat()).use {
                    it.write(responseDocument)
                }
            }

            sqlFilePaths.forEach {
                Files.copy(it, resultDirectory.resolve(it.fileName))
            }

            Properties().also { properties ->
                properties.setProperty("endpoint", resolvedProperties.endpoint)
                properties.setProperty("ignore", resolvedProperties.ignore.toString())
            }.store(Files.newBufferedWriter(resultPropertiesPath), "Properties for the ${it.fileName} test")
        }
    }

    private fun getLeafDirectories(current: Path): MutableList<Path> {
        val result = mutableListOf<Path>()

        Files.walkFileTree(current, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult {
                if (Files.list(directory).noneMatch { Files.isDirectory(it) }) {
                    result.add(directory)
                }

                return FileVisitResult.CONTINUE
            }
        })

        return result
    }

    private fun resolveProperties(testDirectory: Path): TimProperties {
        var currentDirectory = testDirectory

        var endpoint: String?
        var ignore: Boolean?

        while (!currentDirectory.endsWith("src/main/test")) {
            val propertiesPath = currentDirectory.resolve("config.properties")

            if (Files.exists(propertiesPath)) {
                val properties = Properties().apply { load(Files.newInputStream(propertiesPath)) }

                endpoint = properties.getProperty("endpoint")
                ignore = properties.getProperty("ignore").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        null -> false
                        else -> throw GradleException("Invalid value for ignore property: $it")
                    }
                }

                if (endpoint != null) {
                    return TimProperties(endpoint, ignore == true)
                }
            }

            currentDirectory = currentDirectory.parent
        }

        throw GradleException("No config.properties file with the required properties on the path of test: ${testDirectory.fileName}")
    }

    private fun getRelevantPathsForTest(directory: Path): Triple<Path, Path, List<Path>> {
        var requestPath: Path? = null
        var responsePath: Path? = null
        val sqlFilePaths = mutableListOf<Path>()

        Files.list(directory).forEach { path ->
            when (path.fileName.toString()) {
                "request.xml" -> requestPath = path
                "response.xml" -> responsePath = path
                "timdb_pre.sql", "timdb_post.sql", "timstat_pre.sql", "timstat_post.sql" -> sqlFilePaths.add(path)
                "config.properties" -> Unit
                else -> logger.warn("Ignoring unknown file: ${path.fileName}")
            }
        }

        requestPath?.let { safeRequestFile ->
            responsePath?.let { safeResponseFile ->
                return Triple(safeRequestFile, safeResponseFile, sqlFilePaths)
            }

            throw GradleException("Missing response.xml for test: ${directory.fileName}")
        }

        throw GradleException("Missing request.xml for test: ${directory.fileName}")
    }

    private fun transformRequest(request: Document): Document {
        return request
    }

    private fun transformResponse(response: Document): Document {
        return response
    }
}