package de.smartsquare.squit.report

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
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

    private const val bootstrapPath = "META-INF/resources/webjars/bootstrap/3.3.7-1"
    private const val fontAwesomePath = "META-INF/resources/webjars/font-awesome/4.7.0"
    private const val awesomeBootstrapCheckboxPath = "META-INF/resources/webjars/awesome-bootstrap-checkbox/0.3.7"
    private const val jqueryPath = "META-INF/resources/webjars/jquery/1.11.1"
    private const val diff2htmlPath = "META-INF/resources/webjars/diff2html/2.3.2"

    private val resources = arrayOf(
        "$bootstrapPath/css/bootstrap.min.css" to "css/bootstrap.css",
        "$bootstrapPath/js/bootstrap.min.js" to "js/bootstrap.js",
        "$fontAwesomePath/css/font-awesome.min.css" to "css/font-awesome.css",
        "$fontAwesomePath/fonts/fontawesome-webfont.eot" to "fonts/fontawesome-webfont.eot",
        "$fontAwesomePath/fonts/fontawesome-webfont.svg" to "fonts/fontawesome-webfont.svg",
        "$fontAwesomePath/fonts/fontawesome-webfont.ttf" to "fonts/fontawesome-webfont.ttf",
        "$fontAwesomePath/fonts/fontawesome-webfont.woff" to "fonts/fontawesome-webfont.woff",
        "$fontAwesomePath/fonts/fontawesome-webfont.woff2" to "fonts/fontawesome-webfont.woff2",
        "$awesomeBootstrapCheckboxPath/awesome-bootstrap-checkbox.css" to "css/awesome-bootstrap-checkbox.css",
        "$jqueryPath/jquery.min.js" to "js/jquery.js",
        "$diff2htmlPath/dist/diff2html.min.css" to "css/diff2html.css",
        "$diff2htmlPath/dist/diff2html.min.js" to "js/diff2html.js",
        "$diff2htmlPath/dist/diff2html-ui.min.js" to "js/diff2html-ui.js",
        "squit.css" to "css/squit.css",
        "squit.js" to "js/squit.js"
    )

    private val emptyDiffHeader = listOf("--- $DIFF_FILE_NAME", "+++ $DIFF_FILE_NAME", "@@ -1 +1 @@")

    /**
     * Generates and writes the Squit html report, given the [results] list and [reportDirectoryPath].
     */
    fun writeReport(results: List<SquitResult>, reportDirectoryPath: Path) {
        val document = StringBuilder("<!DOCTYPE html>").appendHTML().html {
            squitHead()
            squitBody(results)
        }

        results.forEach { result ->
            val unifiedDiffForJs = generateDiff(result).joinToString("\\n\\\n").replace("'", "\\'")
            val detailDocument = StringBuilder("<!DOCTYPE html>").appendHTML().html {
                squitDetailHead()
                squitDetailBody()
            }

            val detailPath = reportDirectoryPath.resolve("detail").resolve(result.id.toString())
            val detailHtmlPath = detailPath.resolve("detail.html")
            val detailCssPath = detailPath.resolve("detail.css")
            val detailJsPath = detailPath.resolve("detail.js")

            Files.createDirectories(detailPath)
            Files.write(detailHtmlPath, detailDocument.toString().toByteArray())

            FilesUtils.copyResource("squit-detail.css", detailCssPath)
            FilesUtils.copyResource("squit-detail.js", detailJsPath, {
                it.toString(Charset.defaultCharset())
                    .replace(Regex("diffPlaceholder"), Matcher.quoteReplacement(unifiedDiffForJs))
                    .toByteArray()
            })
        }

        resources.forEach { (name, target) ->
            FilesUtils.copyResource(name, reportDirectoryPath.resolve(target))
        }

        Files.write(reportDirectoryPath.resolve("main.html"), document.toString().toByteArray())
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
