package de.smartsquare.squit

import com.google.gson.JsonElement
import com.typesafe.config.Config
import de.smartsquare.squit.interfaces.SquitJsonPostProcessor

class JsonPostProcessor : SquitJsonPostProcessor {

    override fun process(actualResponse: JsonElement, expectedResponse: JsonElement, config: Config) {
        actualResponse.asJsonObject.addProperty("post", config.getString("mediaType"))
    }
}
