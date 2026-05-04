package de.smartsquare.squit.mediatype.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.smartsquare.squit.mediatype.Canonicalizer
import de.smartsquare.squit.mediatype.MediaTypeConfig
import de.smartsquare.squit.util.prettyGson

/**
 * [Canonicalizer] for JSON.
 */
class JsonCanonicalizer : Canonicalizer {

    override fun canonicalize(input: String, mediaTypeConfig: MediaTypeConfig): String =
        if (mediaTypeConfig.jsonCanonicalize) {
            val element = prettyGson.fromJson(input, JsonElement::class.java)

            prettyGson.toJson(element.canonicalize())
        } else {
            input
        }

    private fun JsonElement.canonicalize(): JsonElement = when (this) {
        is JsonObject -> JsonObject().also { newObject ->
            entrySet()
                .sortedBy { (key, _) -> key }
                .forEach { (key, value) -> newObject.add(key, value.canonicalize()) }
        }

        is JsonArray -> JsonArray().also { newArray ->
            forEach { newArray.add(it.canonicalize()) }
        }

        else -> this
    }
}
