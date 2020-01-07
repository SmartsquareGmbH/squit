package de.smartsquare.squit.task

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.cut
import java.nio.file.Files
import java.nio.file.Path

/**
 * Helper object for the [SquitPostProcessTask].
 */
object SquitPostProcessRunner {

    /**
     * Runs the post processing.
     */
    fun run(
        processedSourcesPath: Path,
        actualResponsesPath: Path,
        processedActualResponsesPath: Path,
        testPath: Path
    ) {
        val resultActualResponsePath = Files.createDirectories(
            processedActualResponsesPath.resolve(testPath.cut(actualResponsesPath))
        )

        val errorFile = testPath.resolve(Constants.ERROR)

        if (Files.exists(errorFile)) {
            Files.copy(errorFile, resultActualResponsePath.resolve(Constants.ERROR))
        } else {
            val configPath = FilesUtils
                .validateExistence(processedSourcesPath.resolve(testPath.cut(actualResponsesPath)))
                .resolve(Constants.CONFIG)

            val config = ConfigFactory.parseFile(configPath.toFile())

            val actualResponsePath = FilesUtils.validateExistence(
                testPath.resolve(MediaTypeFactory.actualResponse(config.mediaType))
            )

            val expectedResponsePath = FilesUtils.validateExistence(
                processedSourcesPath
                    .resolve(testPath.cut(actualResponsesPath))
                    .resolve(MediaTypeFactory.expectedResponse(config.mediaType))
            )

            val resultActualResponseFilePath = resultActualResponsePath
                .resolve(MediaTypeFactory.actualResponse(config.mediaType))

            try {
                MediaTypeFactory.processor(config.mediaType)
                    .postProcess(actualResponsePath, expectedResponsePath, resultActualResponseFilePath, config)
            } catch (error: Throwable) {
                Files.write(resultActualResponsePath.resolve(Constants.ERROR), error.toString().toByteArray())
            }
        }
    }
}
