package de.smartsquare.squit.mediatype.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.smartsquare.squit.SquitExtension
import de.smartsquare.squit.mediatype.Canonicalizer

/**
 * [Canonicalizer] for Json.
 */
class JsonCanonicalizer : Canonicalizer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun canonicalize(input: String, extension: SquitExtension): String {
        return if (extension.json.canonicalize) {
            val element = gson.fromJson(input, JsonElement::class.java)

            gson.toJson(element.canonicalize())
        } else {
            input
        }
    }

    private fun JsonElement.canonicalize(): JsonElement {
        return when (this) {
            is JsonObject -> {
                val newEntries = entrySet().map { (key, value) -> key to value.canonicalize() }

                JsonObject().also { newObject ->
                    newEntries.sortedBy { (key) -> key }.forEach { (key, value) ->
                        newObject.add(key, value)
                    }
                }
            }
            is JsonArray -> {
                val newEntries = this.map { it.canonicalize() }

                JsonArray().also { newArray ->
                    newEntries.forEach {
                        newArray.add(it)
                    }
                }
            }
            else -> this
        }
    }
}
