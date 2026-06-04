package de.smartsquare.squit.report

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.mediatype.MediaTypeFactory
import de.smartsquare.squit.util.gson
import okhttp3.MediaType
import org.gradle.api.logging.Logger

/**
 * Writes a [SquitReportResultBranch] tree to a [JsonWriter]. The tree itself only holds [SquitResult] references at
 * its leaves; the response files are read and canonicalized lazily here so the full report payload never lives in
 * memory at once.
 */
class SquitReportResultBranchAdapter(private val mediaTypeConfig: MediaTypeConfig, private val logger: Logger) :
    TypeAdapter<SquitReportResultBranch>() {

    override fun write(out: JsonWriter, value: SquitReportResultBranch) {
        out.beginObject()

        for ((key, child) in value.children) {
            out.name(key)

            when (child) {
                is SquitReportResultBranch -> write(out, child)

                is SquitReportResultLeaf -> gson.toJson(
                    buildReportResult(child.result),
                    SquitReportResult::class.java,
                    out,
                )
            }
        }

        out.endObject()
    }

    override fun read(input: JsonReader) = throw UnsupportedOperationException()

    private fun buildReportResult(result: SquitResult): SquitReportResult {
        val canonicalizedExpected = if (!result.isError) {
            canonicalize(result.expectedContent(), result.mediaType, "Could not canonicalize expected response")
        } else {
            result.expectedContent()
        }

        val canonicalizedActual = if (!result.isError) {
            canonicalize(result.actualContent(), result.mediaType, "Could not canonicalize actual response")
        } else {
            result.actualContent()
        }

        val hasInfoDiff = !result.expectedResponseInfo.isDefault && !result.isError

        return SquitReportResult(
            id = result.id,
            alternativeName = result.alternativeName,
            description = result.description,
            success = result.isSuccess,
            ignored = result.isIgnored,
            error = result.isError,
            duration = result.metaInfo.duration,
            expected = canonicalizedExpected,
            actual = canonicalizedActual,
            infoExpected = if (hasInfoDiff) result.expectedResponseInfo.toJson() else null,
            infoActual = if (hasInfoDiff) result.actualInfoContent() else null,
            language = highlightLanguage(result.mediaType),
        )
    }

    private fun canonicalize(content: String, mediaType: MediaType, errorMessage: String): String = when {
        content.isEmpty() -> content

        else -> try {
            MediaTypeFactory.canonicalizer(mediaType).canonicalize(content, mediaTypeConfig)
        } catch (error: Exception) {
            logger.warn(errorMessage, error)

            content
        }
    }

    private fun highlightLanguage(mediaType: MediaType): String? = when (mediaType) {
        MediaTypeFactory.xmlMediaType, MediaTypeFactory.applicationXmlMediaType, MediaTypeFactory.soapMediaType -> "xml"
        MediaTypeFactory.jsonMediaType -> "json"
        else -> null
    }
}
