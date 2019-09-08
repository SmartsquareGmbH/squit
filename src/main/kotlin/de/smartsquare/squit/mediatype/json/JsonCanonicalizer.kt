package de.smartsquare.squit.mediatype.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import de.smartsquare.squit.mediatype.Canonicalizer

/**
 * [Canonicalizer] for Json.
 *
 * @author Ruben Gees
 */
class JsonCanonicalizer : Canonicalizer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun canonicalize(input: String): String {
        val element = gson.fromJson(input, JsonElement::class.java)

        return gson.toJson(element.canonicalize())
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
            is JsonPrimitive -> {
                if (this.isNumber) {
                    val number = this.asBigDecimal

                    val normalizedNumber: Number = try {
                        number.toBigIntegerExact()
                    } catch (error: ArithmeticException) {
                        number
                    }

                    JsonPrimitive(normalizedNumber)
                } else {
                    this
                }
            }
            else -> this
        }
    }
}
