package de.smartsquare.squit.report

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import de.smartsquare.squit.entity.SquitResponseInfo
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.io.FilesUtils
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * Object for writing the Squit html report.
 *
 * @author Ruben Gees
 */
object HtmlReportWriter {

    private const val DIFF_FILE_NAME = "Result"
    private const val DIFF_INFO_FILE_NAME = "ResultInfo"
    private const val DIFF_CONTEXT_SIZE = 1_000_000
    private const val HTML_LINE_ENDING = "\\n\\\n"

    private const val bootstrapPath = "META-INF/resources/webjars/bootstrap/4.3.1/dist"
    private const val fontAwesomePath = "META-INF/resources/webjars/font-awesome/5.8.2"
    private const val jqueryPath = "META-INF/resources/webjars/jquery/3.4.1/dist"
    private const val popperJsPath = "META-INF/resources/webjars/popper.js/1.15.0/dist/umd"
    private const val markedPath = "META-INF/resources/webjars/marked/0.6.2"
    private const val diff2htmlPath = "META-INF/resources/webjars/diff2html/2.7.0"

    private val resources = arrayOf(
        "$bootstrapPath/css/bootstrap.min.css" to "css/bootstrap.css",
        "$bootstrapPath/js/bootstrap.min.js" to "js/bootstrap.js",
        "$fontAwesomePath/js/all.min.js" to "js/fontawesome.js",
        "$jqueryPath/jquery.slim.min.js" to "js/jquery.js",
        "$popperJsPath/popper.min.js" to "js/popper.js",
        "$markedPath/marked.min.js" to "js/marked.js",
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
            val detailDocument = StringBuilder("<!doctype html>").appendHTML().html {
                squitDetailHead()
                squitDetailBody(result)
            }

            val detailPath = reportDirectoryPath.resolve("detail").resolve(result.id.toString())
            val detailHtmlPath = detailPath.resolve("detail.html")
            val detailCssPath = detailPath.resolve("detail.css")
            val detailJsPath = detailPath.resolve("detail.js")

            val bodyDiff = generateDiff(result.expectedLines, result.actualLines, DIFF_FILE_NAME)
            val unifiedDiffForJs = prepareForJs(bodyDiff)

            val unifiedInfoDiffForJs = if (!result.expectedResponseInfo.isDefault) {
                val expectedInfoLines = result.expectedResponseInfo.toJson().lines()
                val actualInfo = result.actualInfoLines.joinToString(separator = "\n")
                val actualInfoLines = SquitResponseInfo.fromJson(actualInfo).toJson().lines()
                val infoDiff = generateDiff(expectedInfoLines, actualInfoLines, DIFF_INFO_FILE_NAME)
                prepareForJs(infoDiff)
            } else {
                ""
            }

            val descriptionForReplacement = if (result.description == null) "null" else "\"${result.description}\""
                .replace("\n", HTML_LINE_ENDING)

            Files.createDirectories(detailPath)
            Files.write(detailHtmlPath, detailDocument.toString().toByteArray())

            FilesUtils.copyResource("squit-detail.css", detailCssPath)
            FilesUtils.copyResource("squit-detail.js", detailJsPath) {
                it.toString(Charset.defaultCharset())
                    .replace("infoDiffPlaceholder", unifiedInfoDiffForJs)
                    .replace("diffPlaceholder", unifiedDiffForJs)
                    .replace("namePlaceholder", result.simpleName)
                    .replace("alternativeNamePlaceholder", result.alternativeName)
                    .replace("\"descriptionPlaceholder\"", descriptionForReplacement)
                    .toByteArray()
            }
        }

        resources.forEach { (name, target) ->
            FilesUtils.copyResource(name, reportDirectoryPath.resolve(target))
        }

        Files.write(reportDirectoryPath.resolve("index.html"), document.toString().toByteArray())
    }

    private fun prepareForJs(bodyDiff: List<String>): String {
        return bodyDiff
            .joinToString(HTML_LINE_ENDING)
            .replace("'", "\\'")
            .replace("\"", "\\\"")
    }

    private fun generateDiff(expectedLines: List<String>, actualLines: List<String>, filename: String): List<String> {
        val diff = DiffUtils.diff(expectedLines, actualLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
            filename,
            filename,
            expectedLines,
            diff,
            DIFF_CONTEXT_SIZE
        )

        return when (unifiedDiff.isEmpty()) {
            true -> emptyDiffHeader.plus(actualLines.map { " $it" })
            false -> unifiedDiff
        }
    }
}
