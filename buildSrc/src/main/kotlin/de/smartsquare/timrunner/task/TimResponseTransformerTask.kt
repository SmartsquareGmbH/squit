package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.FilesUtils
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.read
import de.smartsquare.timrunner.util.write
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class TimResponseTransformerTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var inputSourceDirectory: Path = Paths.get(project.buildDir.path, "source")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var inputResponseDirectory: Path = Paths.get(project.buildDir.path, "results/raw")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory: Path = Paths.get(project.buildDir.path, "results/processed")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.getLeafDirectories(inputResponseDirectory).forEach { testDir ->
            val responsePath = FilesUtils.validateExistence(testDir.resolve(RESPONSE))
            val expectedResponsePath = FilesUtils.validateExistence(inputSourceDirectory
                    .resolve(testDir.cut(inputResponseDirectory))
                    .resolve(RESPONSE))

            val resultDirectoryPath = Files.createDirectories(outputDirectory.
                    resolve(testDir.cut(inputResponseDirectory)))

            val resultFilePath = FilesUtils.createFileIfNotExists(resultDirectoryPath.resolve(RESPONSE))

            val response = SAXReader().read(responsePath)
            val expectedResponse = SAXReader().read(expectedResponsePath)

            transform(response, expectedResponse)

            response.write(resultFilePath)
        }
    }

    private fun transform(response: Document, expectedResponse: Document) {
        if (response.selectNodes("Fault").isEmpty()) {
            expectedResponse.selectSingleNode("//TransactionId")?.let {
                response.selectSingleNode("//TransactionId")?.text = it.text
            }
        }
    }
}
