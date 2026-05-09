package de.smartsquare.squit

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.typesafe.config.Config
import de.smartsquare.squit.interfaces.SquitJsonPostProcessor

class JsonArrayPostProcessor : SquitJsonPostProcessor {

    override fun process(actualResponse: JsonElement, expectedResponse: JsonElement, config: Config) {
        actualResponse.asJsonArray.add(JsonPrimitive(config.getString("mediaType")))
    }
}
