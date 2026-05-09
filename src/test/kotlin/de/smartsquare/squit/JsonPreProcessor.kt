package de.smartsquare.squit

import com.google.gson.JsonElement
import com.typesafe.config.Config
import de.smartsquare.squit.interfaces.SquitJsonPreProcessor

class JsonPreProcessor : SquitJsonPreProcessor {

    override fun process(request: JsonElement?, expectedResponse: JsonElement, config: Config) {
        request?.asJsonObject?.addProperty("pre", true)
    }
}
