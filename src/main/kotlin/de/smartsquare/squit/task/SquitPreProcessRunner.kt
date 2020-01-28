package de.smartsquare.squit.task

import de.smartsquare.squit.config.mediaType
import de.smartsquare.squit.config.writeTo
import de.smartsquare.squit.entity.SquitTest
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.Constants
import de.smartsquare.squit.util.cut
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Helper object for the [SquitPreProcessTask].
 */
object SquitPreProcessRunner {

    private const val TRANSFER_BUFFER_SIZE = 8192

    /**
     * Runs the pre processing.
     */
    fun run(sourceDir: Path, processedSourcesPath: Path, test: SquitTest) {
        val mediaType = test.config.mediaType

        val processedResultPath = Files.createDirectories(processedSourcesPath.resolve(test.path.cut(sourceDir)))
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
                        reader.transferToCompat(writer)

                        if (index < inputs.lastIndex && separator.isNotEmpty()) {
                            writer.write(separator)
                        }
                    }
                }
            }
    }

    // This method is only available since Java 10.
    private fun Reader.transferToCompat(out: Writer): Long {
        val buffer = CharArray(TRANSFER_BUFFER_SIZE)

        var transferred: Long = 0
        var nRead: Int

        while (read(buffer, 0, TRANSFER_BUFFER_SIZE).also { nRead = it } >= 0) {
            out.write(buffer, 0, nRead)
            transferred += nRead.toLong()
        }

        return transferred
    }
}
