package de.smartsquare.squit.task

import de.smartsquare.squit.SquitPluginExtension
import de.smartsquare.squit.SquitPostProcessor
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.Constants.ACTUAL_RESPONSE
import de.smartsquare.squit.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.squit.util.cut
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.write
import org.dom4j.io.SAXReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task for post-processing the responses.
 *
 * @author Ruben Gees
 */
open class SquitPostProcessTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var processedSourcesPath: Path = Paths.get(project.buildDir.path, "sources")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var actualResponsesPath: Path = Paths.get(project.buildDir.path, "responses", "raw")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var processedActualResponsesPath: Path = Paths.get(project.buildDir.path, "responses", "processed")

    @get:Internal
    private val processor by lazy {
        project.extensions.getByType(SquitPluginExtension::class.java).postProcessClass.let {
            if (it.isNotBlank()) {
                logger.info("Using $it for post processing.")

                Class.forName(it).newInstance() as SquitPostProcessor
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
        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { testDir ->
            val actualResponsePath = FilesUtils.validateExistence(testDir.resolve(ACTUAL_RESPONSE))
            val expectedResponsePath = FilesUtils.validateExistence(processedSourcesPath
                    .resolve(testDir.cut(actualResponsesPath))
                    .resolve(EXPECTED_RESPONSE))

            val resultProcessedActualResponsePath = Files.createDirectories(processedActualResponsesPath
                    .resolve(testDir.cut(actualResponsesPath)))

            val resultProcessedActualResponseFilePath = FilesUtils
                    .createFileIfNotExists(resultProcessedActualResponsePath.resolve(ACTUAL_RESPONSE))

            val actualResponse = SAXReader().read(actualResponsePath)
            val expectedResponse = SAXReader().read(expectedResponsePath)

            processor?.process(actualResponse, expectedResponse)

            actualResponse.write(resultProcessedActualResponseFilePath)
        }
    }
}
