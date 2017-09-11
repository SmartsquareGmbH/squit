package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.util.Constants.CONFIG
import de.smartsquare.timrunner.util.Constants.REQUEST
import de.smartsquare.timrunner.util.Constants.RESPONSE
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_POST
import de.smartsquare.timrunner.util.Constants.TAXBASE_DB_PRE
import de.smartsquare.timrunner.util.Constants.TIM_DB_POST
import de.smartsquare.timrunner.util.Constants.TIM_DB_PRE
import de.smartsquare.timrunner.util.FilesUtils
import de.smartsquare.timrunner.util.cut
import de.smartsquare.timrunner.util.safeCleanedStringValueAt
import de.smartsquare.timrunner.util.safeStore
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

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
    var outputDirectory: Path = Paths.get(project.projectDir.path, "src/main/test/supply-chain")

    @Internal
    private val order = arrayOf("Validation", "Grouping", "DecideTaxTreatment&Consolidation", "Calculation",
            "Finalization", "Smoke Tests", "Smoke Tests - New Algo")

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        outputDirectory.toFile().deleteRecursively()

        var currentIndex = 0

        FilesUtils.getChildDirectories(inputDirectory)
                .sortedWith(kotlin.Comparator { first, second ->
                    order.indexOf(first.fileName.toString()).compareTo(order.indexOf(second.fileName.toString()))
                })
                .forEach { testDirectoryPath ->
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
                        currentFirstPath = row.safeCleanedStringValueAt(0) ?: currentFirstPath
                        currentSecondPath = row.safeCleanedStringValueAt(1) ?: currentSecondPath
                        currentThirdPath = row.safeCleanedStringValueAt(2) ?: currentThirdPath

                        val requestName = row.safeCleanedStringValueAt(5)
                        val responseName = row.safeCleanedStringValueAt(6)

                        if (requestName != null && responseName != null) {
                            val requestFilePath = FilesUtils.validateExistence(testDirectoryPath
                                    .resolve("Input")
                                    .resolve("$requestName.xml"))

                            val responseFilePath = FilesUtils.validateExistence(testDirectoryPath
                                    .resolve("Output")
                                    .resolve("$responseName.xml"))

                            val resultApiDirectoryPath = Files.createDirectories(outputDirectory
                                    .resolve(testDirectoryPath.cut(inputDirectory))
                                    .resolve(currentFirstPath))

                            val resultDirectoryPath = Files.createDirectories(resultApiDirectoryPath
                                    .resolve(currentSecondPath)
                                    .resolve(currentThirdPath)
                                    .resolve(formatResponseName(currentIndex, requestName)))

                            copyRequestAndResponse(resultDirectoryPath, requestFilePath, responseFilePath)
                            copyDatabaseScripts(row, testDirectoryPath, resultDirectoryPath)
                            generateProperties(resultApiDirectoryPath, currentFirstPath)

                            currentIndex += 1
                        } else {
                            logger.warn("Skipped test $currentFirstPath/$currentSecondPath/$currentThirdPath " +
                                    "in xls file: $xlsFile")
                        }
                    }
                }
    }

    private fun copyRequestAndResponse(resultDirectoryPath: Path, requestFilePath: Path, responseFilePath: Path) {
        Files.write(resultDirectoryPath.resolve(REQUEST), Files.readAllBytes(requestFilePath)
                .toString(Charsets.UTF_8)
                .toByteArray())

        Files.write(resultDirectoryPath.resolve(RESPONSE), Files.readAllBytes(responseFilePath)
                .toString(Charsets.UTF_8)
                .replace("ns0", "ns2")
                .toByteArray())
    }

    private fun copyDatabaseScripts(row: Row, testDirectoryPath: Path, resultDirectoryPath: Path) {
        row.safeCleanedStringValueAt(3)?.let {
            copyDatabaseScript(it, TIM_DB_PRE, testDirectoryPath.resolve("Input"),
                    resultDirectoryPath)
        }

        row.safeCleanedStringValueAt(4)?.let {
            copyDatabaseScript(it, TAXBASE_DB_PRE, testDirectoryPath.resolve("Input"),
                    resultDirectoryPath)
        }

        row.safeCleanedStringValueAt(7)?.let {
            copyDatabaseScript(it, TIM_DB_POST, testDirectoryPath.resolve("Output"),
                    resultDirectoryPath)
        }

        row.safeCleanedStringValueAt(8)?.let {
            copyDatabaseScript(it, TAXBASE_DB_POST, testDirectoryPath.resolve("Output"),
                    resultDirectoryPath)
        }
    }

    private fun generateProperties(resultApiDirectoryPath: Path, currentFirstPath: String) {
        if (Files.notExists(outputDirectory.resolve(CONFIG))) {
            FilesUtils.createFileIfNotExists(outputDirectory.resolve(CONFIG)).let {
                generateDefaultProperties().safeStore(it)
            }
        }

        if (Files.notExists(resultApiDirectoryPath.resolve(CONFIG))) {
            FilesUtils.createFileIfNotExists(resultApiDirectoryPath.resolve(CONFIG)).let {
                Properties().apply {
                    when {
                        currentFirstPath.contains("Apply Tax") -> setProperty("endpoint",
                                "http://localhost:7001/tim/ApplyTaxWSSoap12HttpPort?WSDL")
                        currentFirstPath.contains("Control Tax") -> setProperty("endpoint",
                                "http://localhost:7001/tim/ControlTaxWSSoapHttpPort?WSDL")
                        else -> {
                            setProperty("endpoint", "http://localhost:7001/dummy")
                            setProperty("ignore", "true")

                            logger.warn("Unable to determine endpoint fo suite " +
                                    "${resultApiDirectoryPath.cut(outputDirectory)}, ignoring.")
                        }
                    }
                }.safeStore(it)
            }
        }
    }

    private fun formatResponseName(currentIndex: Int, name: String) = currentIndex.toString() + "-" + name
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

    private fun generateDefaultProperties(): Properties {
        return Properties().apply {
            setProperty("timdb_jdbc", "jdbc:oracle:thin:@localhost:1521:xe")
            setProperty("timdb_user", "timdb")
            setProperty("timdb_password", "timdb")
            setProperty("taxbasedb_jdbc", "jdbc:oracle:thin:@localhost:1521:xe")
            setProperty("taxbasedb_user", "timdb")
            setProperty("taxbasedb_password", "timdb")
        }
    }
}
