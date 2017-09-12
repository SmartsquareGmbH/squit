package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.io.FilesUtils
import de.smartsquare.timrunner.logic.TimTransformer
import de.smartsquare.timrunner.util.Constants.RESPONSE
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

open class TimPostProcessTask : DefaultTask() {

    /**
     * The directory of the test sources.
     */
    @InputDirectory
    var sourcesPath: Path = Paths.get(project.buildDir.path, "source")

    /**
     * The directory of the previously requested responses.
     */
    @InputDirectory
    var actualResponsesPath: Path = Paths.get(project.buildDir.path, "results/raw")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var processedActualResponsesPath: Path = Paths.get(project.buildDir.path, "results/processed")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.getSortedLeafDirectories(actualResponsesPath).forEach { testDir ->
            val actualResponsePath = FilesUtils.validateExistence(testDir.resolve(RESPONSE))
            val expectedResponsePath = FilesUtils.validateExistence(sourcesPath
                    .resolve(testDir.cut(actualResponsesPath))
                    .resolve(RESPONSE))

            val resultProcessedActualResponsePath = Files.createDirectories(processedActualResponsesPath
                    .resolve(testDir.cut(actualResponsesPath)))

            val resultProcessedActualResponseFilePath = FilesUtils
                    .createFileIfNotExists(resultProcessedActualResponsePath.resolve(RESPONSE))

            val actualResponse = SAXReader().read(actualResponsePath)
            val expectedResponse = SAXReader().read(expectedResponsePath)

            transform(actualResponse, expectedResponse)

            actualResponse.write(resultProcessedActualResponseFilePath)
        }
    }

    private fun transform(actualResponse: Document, expectedResponse: Document) {
        if (actualResponse.selectNodes("//Fault").isEmpty()) {
            TimTransformer.stripStackTraces(actualResponse)
            TimTransformer.replaceErrorTransactionIdFromExpectedResponse(actualResponse, expectedResponse)

            TimTransformer.sortTaxInvoiceSubTotals(actualResponse, "SellerTaxTotal")
            TimTransformer.sortTaxInvoiceSubTotals(actualResponse, "BuyerTaxTotal")

            TimTransformer.replaceTransactionIdFromExpectedResponse(actualResponse, expectedResponse)
        }
    }
}
