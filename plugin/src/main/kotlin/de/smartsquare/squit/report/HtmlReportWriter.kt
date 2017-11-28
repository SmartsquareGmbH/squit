package de.smartsquare.squit.report

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import de.smartsquare.squit.util.write
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.html
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher

/**
 * Object for writing the Squit html report.
 *
 * @author Ruben Gees
 */
object HtmlReportWriter {

    private const val DIFF_FILE_NAME = "Result"
    private const val DIFF_CONTEXT_SIZE = 1000000

    private val bootstrapPath = "META-INF/resources/webjars/bootstrap/3.3.7-1"
    private val jqueryPath = "META-INF/resources/webjars/jquery/1.11.1"
    private val diff2htmlPath = "META-INF/resources/webjars/diff2html/2.3.2"

    private val resources = arrayOf(
            bootstrapPath + "/css/bootstrap.min.css" to "css/bootstrap.css",
            bootstrapPath + "/js/bootstrap.min.js" to "js/bootstrap.js",
            bootstrapPath + "/fonts/glyphicons-halflings-regular.eot" to "fonts/glyphicons-halflings-regular.eot",
            bootstrapPath + "/fonts/glyphicons-halflings-regular.svg" to "fonts/glyphicons-halflings-regular.svg",
            bootstrapPath + "/fonts/glyphicons-halflings-regular.ttf" to "fonts/glyphicons-halflings-regular.ttf",
            bootstrapPath + "/fonts/glyphicons-halflings-regular.woff" to "fonts/glyphicons-halflings-regular.woff",
            bootstrapPath + "/fonts/glyphicons-halflings-regular.woff2" to "fonts/glyphicons-halflings-regular.woff2",
            jqueryPath + "/jquery.min.js" to "js/jquery.js",
            diff2htmlPath + "/dist/diff2html.min.css" to "css/diff2html.css",
            diff2htmlPath + "/dist/diff2html.min.js" to "js/diff2html.js",
            diff2htmlPath + "/dist/diff2html-ui.min.js" to "js/diff2html-ui.js",
            "squit.css" to "css/squit.css",
            "squit.js" to "js/squit.js"
    )

    private val emptyDiffHeader = listOf("--- $DIFF_FILE_NAME", "+++ $DIFF_FILE_NAME", "@@ -1 +1 @@")

    /**
     * Generates and writes the Squit html report, given the [results] list and [reportFilePath].
     */
    fun writeReport(results: List<SquitResult>, reportFilePath: Path) {
        val document = createHTMLDocument().html {
            squitHead()
            squitBody(results)
        }

        results.forEach { result ->
            val unifiedDiffForJs = generateDiff(result).joinToString("\\n\\\n").replace("'", "\\'")
            val detailDocument = createHTMLDocument().html {
                squitDetailHead()
                squitDetailBody()
            }

            val detailPath = reportFilePath.parent.resolve("detail").resolve(result.id.toString())
            val detailHtmlPath = detailPath.resolve("detail.html")
            val detailCssPath = detailPath.resolve("detail.css")
            val detailJsPath = detailPath.resolve("detail.js")

            Files.createDirectories(detailPath)
            Files.createFile(detailHtmlPath)

            detailDocument.write(detailHtmlPath)

            FilesUtils.copyResource("squit-detail.css", detailCssPath)
            FilesUtils.copyResource("squit-detail.js", detailJsPath, {
                it.toString(Charset.defaultCharset())
                        .replace(Regex("diffPlaceholder"), Matcher.quoteReplacement(unifiedDiffForJs))
                        .toByteArray()
            })
        }

        resources.forEach { (name, target) ->
            FilesUtils.copyResource(name, reportFilePath.parent.resolve(target))
        }

        document.write(reportFilePath)
    }

    private fun generateDiff(result: SquitResult): List<String> {
        val diff = DiffUtils.diff(result.expectedLines, result.actualLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(DIFF_FILE_NAME, DIFF_FILE_NAME,
                result.expectedLines, diff, DIFF_CONTEXT_SIZE)

        return when (unifiedDiff.isEmpty()) {
            true -> emptyDiffHeader.plus(result.actualLines.map { " $it" })
            false -> unifiedDiff
        }
    }
}
