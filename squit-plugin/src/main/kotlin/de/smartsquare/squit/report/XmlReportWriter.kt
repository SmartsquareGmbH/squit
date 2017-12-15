package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.util.write
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import java.net.InetAddress
import java.nio.file.Path

/**
 * Object for writing the Squit html report.
 *
 * @author Ruben Gees
 */
object XmlReportWriter {

    /**
     * Generates and writes the Squit html report, given the [result] list and [reportFilePath].
     */
    @Suppress("NestedBlockDepth")
    fun writeReport(result: List<SquitResult>, reportFilePath: Path) {
        val document = DocumentHelper.createDocument()
        val root = document.addElement("testsuites")

        result.groupBy { it.path }.forEach { suite, tests ->
            val suiteElement = root.addElement("testsuite").apply {
                addAttribute("name", suite.toString())
                addAttribute("timestamp", tests.firstOrNull()?.metaInfo?.date?.toString() ?: "")
                addAttribute("hostname", InetAddress.getLocalHost().hostName)
                addAttribute("tests", "${tests.size}")
                addAttribute("failures", "${tests.count { !it.isIgnored && !it.isSuccess }}")
                addAttribute("skipped", "${tests.count { it.isIgnored }}")
                addAttribute("errors", "0")
                addAttribute("time", "${tests.sumBy { it.metaInfo.duration.toInt() } / 1000f}")
            }

            tests.forEach {
                val testElement = suiteElement.addElement("testcase").apply {
                    addAttribute("name", it.name)
                    addAttribute("time", "${it.metaInfo.duration.toInt() / 1000f}")
                }

                if (it.isIgnored) {
                    testElement.addElement("skipped").addText(it.result)
                } else if (!it.isSuccess) {
                    testElement.addElement("failure").addText(it.result)
                }
            }
        }

        document.write(reportFilePath, OutputFormat.createPrettyPrint())
    }
}
