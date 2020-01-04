package de.smartsquare.squit.task

import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.config.writeTo
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.asPath
import de.smartsquare.squit.util.cut
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Worker processing each file of the [SquitPreProcessTask].
 */
@Suppress("UnstableApiUsage")
abstract class SquitPreProcessWorker : WorkAction<SquitPreProcessWorker.PreProcessParameters> {

    private val sourcesPath get() = parameters.sourcesPath.asPath
    private val processedSourcesPath get() = parameters.processedSourcesPath.asPath
    private val test get() = parameters.test.get()

    override fun execute() {
        val mediaType = test.config.mediaType

        val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(test.path.cut(sourcesPath)))
        val processedConfigPath = processedResultPath.resolve(Constants.CONFIG)

        val processedRequestPath = processedResultPath.resolve(MediaTypeFactory.request(mediaType))
        val processedResponsePath = processedResultPath.resolve(MediaTypeFactory.expectedResponse(mediaType))
        val processedDescriptionPath = processedResultPath.resolve(Constants.DESCRIPTION)

        try {
            MediaTypeFactory.processor(mediaType)
                .preProcess(test.request, test.response, processedRequestPath, processedResponsePath, test.config)
        } catch (error: Throwable) {
            Files.write(processedResultPath.resolve(Constants.ERROR), error.toString().toByteArray())
        }

        test.preSqlScripts.filterValues { it.isNotEmpty() }.forEach { (name, scripts) ->
            writeAllTo(scripts, processedResultPath.resolve("${name}_pre.sql"))
        }

        test.postSqlScripts.filterValues { it.isNotEmpty() }.forEach { (name, scripts) ->
            writeAllTo(scripts, processedResultPath.resolve("${name}_post.sql"))
        }

        if (test.descriptions.isNotEmpty()) {
            writeAllTo(test.descriptions, processedDescriptionPath, "\n")
        }

        test.config.writeTo(processedConfigPath)
    }

    private fun writeAllTo(inputs: List<Path>, output: Path, separator: String = "") {
        Files.newBufferedWriter(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)
            .use { writer ->
                inputs.forEachIndexed { index, path ->
                    Files.newBufferedReader(path).use { reader ->
                        reader.transferTo(writer)

                        if (index < inputs.lastIndex && separator.isNotEmpty()) {
                            writer.write(separator)
                        }
                    }
                }
            }
    }

    @Suppress("UndocumentedPublicClass", "UndocumentedPublicProperty")
    interface PreProcessParameters : WorkParameters {
        val sourcesPath: DirectoryProperty
        val processedSourcesPath: DirectoryProperty
        val test: Property<SquitTest>
    }
}
