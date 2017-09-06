package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.Constants.REQUEST
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_POST
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_PRE
import de.smartsquare.timrunner.util.Constants.TIM_DB_POST
import de.smartsquare.timrunner.util.Constants.TIM_DB_PRE
import de.smartsquare.timrunner.util.FilesUtils
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.safeStringValueAt
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

open class TimSupplyChainConverterTask : DefaultTask() {

    /**
     * The directory of the sources to convert.
     */
    @InputDirectory
    var inputDirectory: Path = Paths.get(project.projectDir.path, "src/main/supply-chain")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory: Path = Paths.get(project.projectDir.path, "src/main/test")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        outputDirectory.toFile().deleteRecursively()

        Files.newDirectoryStream(inputDirectory, { Files.isDirectory(it) }).use {
            it.forEach { testDirectoryPath ->
                val xlsFile = FilesUtils.validateExistence(testDirectoryPath
                        .resolve("${testDirectoryPath.fileName}_TC.xls"))

                val workbook = HSSFWorkbook(Files.newInputStream(xlsFile)).also {
                    if (it.numberOfSheets <= 0) {
                        throw GradleException("Invalid xls file: $xlsFile (No sheets found)")
                    }
                }

                val sheet = workbook.getSheetAt(0)
                var currentFirstPath = ""
                var currentSecondPath = ""
                var currentThirdPath = ""

                sheet.rowIterator().asSequence().drop(1).forEach { row ->
                    currentFirstPath = row.safeStringValueAt(0)?.trim() ?: currentFirstPath
                    currentSecondPath = row.safeStringValueAt(1)?.trim() ?: currentSecondPath
                    currentThirdPath = row.safeStringValueAt(2)?.trim() ?: currentThirdPath

                    val requestName = row.safeStringValueAt(5)?.trim()
                    val responseName = row.safeStringValueAt(6)?.trim()

                    if (requestName != null && responseName != null) {
                        val requestFilePath = FilesUtils.validateExistence(testDirectoryPath
                                .resolve("Input")
                                .resolve("$requestName.xml"))

                        val responseFilePath = FilesUtils.validateExistence(testDirectoryPath
                                .resolve("Output")
                                .resolve("$responseName.xml"))

                        val resultDirectoryPath = Files.createDirectories(outputDirectory
                                .resolve(testDirectoryPath.cut(inputDirectory))
                                .resolve(currentFirstPath)
                                .resolve(currentSecondPath)
                                .resolve(currentThirdPath)
                                .resolve(formatResponseName(requestName)))

                        Files.copy(requestFilePath, resultDirectoryPath.resolve(REQUEST),
                                StandardCopyOption.REPLACE_EXISTING)

                        Files.copy(responseFilePath, resultDirectoryPath.resolve(RESPONSE),
                                StandardCopyOption.REPLACE_EXISTING)

                        row.safeStringValueAt(3)?.let {
                            copyDatabaseScript(it, TIM_DB_PRE, testDirectoryPath.resolve("Input"),
                                    resultDirectoryPath)
                        }

                        row.safeStringValueAt(4)?.let {
                            copyDatabaseScript(it, TAXBASE_DB_PRE, testDirectoryPath.resolve("Input"),
                                    resultDirectoryPath)
                        }

                        row.safeStringValueAt(7)?.let {
                            copyDatabaseScript(it, TIM_DB_POST, testDirectoryPath.resolve("Output"),
                                    resultDirectoryPath)
                        }

                        row.safeStringValueAt(8)?.let {
                            copyDatabaseScript(it, TAXBASE_DB_POST, testDirectoryPath.resolve("Output"),
                                    resultDirectoryPath)
                        }
                    } else {
                        logger.warn("Skipped test $currentFirstPath/$currentSecondPath/$currentThirdPath " +
                                "in xls file: $xlsFile")
                    }
                }
            }
        }
    }

    private fun formatResponseName(name: String) = name
            .replace("request", "")
            .replace("req", "")
            .trim('_')

    private fun copyDatabaseScript(sourceName: String, resultName: String, testDirectoryPath: Path,
                                   resultDirectoryPath: Path) {
        FilesUtils.validateExistence(testDirectoryPath
                .resolve("$sourceName.sql"))
                .let { path ->
                    Files.copy(path, resultDirectoryPath.resolve(resultName), StandardCopyOption.REPLACE_EXISTING)
                }
    }
}
