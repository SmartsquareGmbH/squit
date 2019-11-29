package de.smartsquare.squit.task

import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import org.gradle.api.file.DirectoryProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.nio.file.Files

/**
 * Worker processing each file of the [SquitPostProcessTask].
 */
@Suppress("UnstableApiUsage")
abstract class SquitPostProcessWorker : WorkAction<SquitPostProcessWorker.PostProcessParameters> {

    private val processedSourcesPath get() = parameters.processedSourcesPath.asPath
    private val actualResponsesPath get() = parameters.actualResponsesPath.asPath
    private val processedActualResponsesPath get() = parameters.processedActualResponsesPath.asPath

    private val testPath get() = parameters.testPath.asPath

    override fun execute() {
        val resultActualResponsePath = Files.createDirectories(
            processedActualResponsesPath.resolve(testPath.cut(actualResponsesPath))
        )

        val errorFile = testPath.resolve(Constants.ERROR)

        if (Files.exists(errorFile)) {
            Files.copy(errorFile, resultActualResponsePath.resolve(Constants.ERROR))
        } else {
            val configPath = FilesUtils.validateExistence(
                processedSourcesPath.resolve(testPath.cut(actualResponsesPath))
            )
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

    interface PostProcessParameters : WorkParameters {
        val processedSourcesPath: DirectoryProperty
        val actualResponsesPath: DirectoryProperty
        val processedActualResponsesPath: DirectoryProperty
        val testPath: DirectoryProperty
    }
}
