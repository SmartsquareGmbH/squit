package de.smartsquare.timrunner.task

import de.smartsquare.timrunner.io.FilesUtils
import de.smartsquare.timrunner.util.Constants.CONFIG
import de.smartsquare.timrunner.util.Constants.EXPECTED_RESPONSE
import de.smartsquare.timrunner.util.Constants.REQUEST
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
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.*

/**
 * Task for converting a legacy SOAP UI project to a format readable by the tim-it-runner.
 *
 * This task is very specific and should only be used for the supply-chain tests.
 *
 * @author Ruben Gees
 */
open class TimSupplyChainConverterTask : DefaultTask() {

    /**
     * The directory of the sources to convert.
     */
    @InputDirectory
    var inputDirectory: Path = Paths.get(project.projectDir.path, "src", "main", "supply-chain")

    /**
     * The directory to save the results in.
     */
    @OutputDirectory
    var outputDirectory: Path = Paths.get(project.projectDir.path, "src", "main", "test", "supply-chain")

    @Internal
    private val order = arrayOf("Validation", "Grouping", "DecideTaxTreatment&Consolidation", "Calculation",
            "Finalization", "Smoke Tests", "Smoke Tests - New Algo")

    @Internal
    private val aliases = mapOf(
            "DecideTaxTreatment&Consolidation_TC" to "DecideTT&Consolidation_TC",
            "Smoke Tests_TC" to "SmokeTest_TC",
            "Smoke Tests - New Algo_TC" to "SmokeTest_TC_NewAlg"
    )

    /**
     * Runs the task.
     */
    @TaskAction
    fun run() {
        FilesUtils.deleteRecursivelyIfExisting(outputDirectory)
        FilesUtils.getChildDirectories(inputDirectory)
                .sortedWith(kotlin.Comparator { first, second ->
                    order.indexOf(first.fileName.toString()).compareTo(order.indexOf(second.fileName.toString()))
                })
                .forEachIndexed { overallIndex, testDirectoryPath ->
                    val xlsName = "${testDirectoryPath.fileName}_TC"
                    val xlsPath = "${aliases.getOrElse(xlsName, { xlsName })}.xls"
                    val xlsFilePath = FilesUtils.validateExistence(testDirectoryPath.resolve(xlsPath))

                    val workbook = HSSFWorkbook(Files.newInputStream(xlsFilePath)).also {
                        if (it.numberOfSheets <= 0) {
                            throw GradleException("Invalid xls file: $xlsFilePath (No sheets found)")
                        }
                    }

                    val sheet = workbook.getSheetAt(0)

                    var currentFirstPath = ""
                    var currentSecondPath = ""
                    var currentThirdPath = ""

                    var currentFirstIndex = -1
                    var currentSecondIndex = -1
                    var currentThirdIndex = -1

                    var testIndex = 0

                    sheet.rowIterator().asSequence().drop(1).forEach { row ->
                        val requestName = row.safeCleanedStringValueAt(5)
                        val responseName = row.safeCleanedStringValueAt(6)

                        if (requestName != null && responseName != null) {
                            row.safeCleanedStringValueAt(0)?.let {
                                currentFirstPath = it
                                currentFirstIndex++

                                currentSecondIndex = if (row.safeCleanedStringValueAt(1) == null) 0 else -1
                                currentThirdIndex = if (row.safeCleanedStringValueAt(2) == null) 0 else -1
                            }

                            row.safeCleanedStringValueAt(1)?.let {
                                currentSecondPath = it
                                currentSecondIndex++

                                currentThirdIndex = if (row.safeCleanedStringValueAt(2) == null) 0 else -1
                            }

                            row.safeCleanedStringValueAt(2)?.let {
                                currentThirdPath = it
                                currentThirdIndex++
                                testIndex = 0
                            }

                            val requestFilePath = FilesUtils.validateExistence(testDirectoryPath
                                    .resolve("Input")
                                    .resolve("$requestName.xml"))

                            val responseFilePath = FilesUtils.validateExistence(testDirectoryPath
                                    .resolve("Output")
                                    .resolve("$responseName.xml"))

                            val resultApiDirectoryPath = Files.createDirectories(outputDirectory
                                    .resolve("$overallIndex-${testDirectoryPath.cut(inputDirectory)}")
                                    .resolve("$currentFirstIndex-$currentFirstPath"))

                            val resultDirectoryPath = Files.createDirectories(resultApiDirectoryPath
                                    .resolve("$currentSecondIndex-$currentSecondPath")
                                    .resolve("$currentThirdIndex-$currentThirdPath")
                                    .resolve(formatResponseName(testIndex, requestName)))

                            copyRequestAndResponse(resultDirectoryPath, requestFilePath, responseFilePath)
                            copyDatabaseScripts(row, testDirectoryPath, resultDirectoryPath)
                            generateProperties(resultApiDirectoryPath, currentFirstPath)

                            testIndex++
                        } else {
                            logger.warn("Skipped test $currentFirstPath/$currentSecondPath/$currentThirdPath " +
                                    "in xls file $xlsFilePath")
                        }
                    }
                }
    }

    private fun copyRequestAndResponse(resultDirectoryPath: Path, requestFilePath: Path, responseFilePath: Path) {
        Files.write(resultDirectoryPath.resolve(REQUEST), Files.readAllBytes(requestFilePath)
                .toString(Charsets.UTF_8)
                .toByteArray())

        Files.write(resultDirectoryPath.resolve(EXPECTED_RESPONSE), Files.readAllBytes(responseFilePath)
                .toString(Charsets.UTF_8)
                .replace("ns0", "ns2")
                .toByteArray())
    }

    private fun copyDatabaseScripts(row: Row, testDirectoryPath: Path, resultDirectoryPath: Path) {
        listOf(
                Triple(3, "tim_pre.sql", testDirectoryPath.resolve("Input")),
                Triple(4, "taxbase_pre.sql", testDirectoryPath.resolve("Input")),
                Triple(7, "tim_post.sql", testDirectoryPath.resolve("Output")),
                Triple(8, "taxbase_post.sql", testDirectoryPath.resolve("Output"))
        ).forEach { (index, name, resolvedTestDirectoryPath) ->
            row.safeCleanedStringValueAt(index)?.let {
                copyDatabaseScript(it, name, resolvedTestDirectoryPath, resultDirectoryPath)
            }
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
                .let { path -> Files.copy(path, resultDirectoryPath.resolve(resultName), REPLACE_EXISTING) }
    }

    private fun generateDefaultProperties(): Properties {
        return Properties().apply {
            setProperty("db_tim_jdbc", "jdbc:oracle:thin:@localhost:1521:xe")
            setProperty("db_tim_username", "timdb")
            setProperty("db_tim_password", "timdb")
            setProperty("db_taxbase_jdbc", "jdbc:oracle:thin:@localhost:1521:xe")
            setProperty("db_taxbase_username", "timdb")
            setProperty("db_taxbase_password", "timdb")
        }
    }
}
