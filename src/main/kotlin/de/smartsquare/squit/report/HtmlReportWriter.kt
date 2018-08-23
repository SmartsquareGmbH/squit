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

    private const val bootstrapPath = "META-INF/resources/webjars/bootstrap/4.1.3"
    private const val fontAwesomePath = "META-INF/resources/webjars/font-awesome/5.2.0"
    private const val jqueryPath = "META-INF/resources/webjars/jquery/3.3.1-1"
    private const val popperJsPath = "META-INF/resources/webjars/popper.js/1.14.3/umd"
    private const val diff2htmlPath = "META-INF/resources/webjars/diff2html/2.3.2"

    private val resources = arrayOf(
        "$bootstrapPath/css/bootstrap.min.css" to "css/bootstrap.css",
        "$bootstrapPath/js/bootstrap.min.js" to "js/bootstrap.js",
        "$fontAwesomePath/css/all.min.css" to "css/fontawesome.css",
        "$fontAwesomePath/js/all.min.js" to "js/fontawesome.js",
        "$jqueryPath/jquery.min.js" to "js/jquery.js",
        "$popperJsPath/popper.min.js" to "js/popper.js",
        "$diff2htmlPath/dist/diff2html.min.css" to "css/diff2html.css",
        "$diff2htmlPath/dist/diff2html.min.js" to "js/diff2html.js",
        "$diff2htmlPath/dist/diff2html-ui.min.js" to "js/diff2html-ui.js",
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
            val detailDocument = StringBuilder("<!doctype html>").appendHTML().html {
                squitDetailHead()
                squitDetailBody()
            }

            val detailPath = reportDirectoryPath.resolve("detail").resolve(result.id.toString())
            val detailHtmlPath = detailPath.resolve("detail.html")
            val detailJsPath = detailPath.resolve("detail.js")

            Files.createDirectories(detailPath)
            Files.write(detailHtmlPath, detailDocument.toString().toByteArray())

            FilesUtils.copyResource("squit-detail.js", detailJsPath) {
                it.toString(Charset.defaultCharset())
                    .replace(Regex("diffPlaceholder"), Matcher.quoteReplacement(unifiedDiffForJs))
                    .replace("titlePlaceholder", result.name)
                    .toByteArray()
            }
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
