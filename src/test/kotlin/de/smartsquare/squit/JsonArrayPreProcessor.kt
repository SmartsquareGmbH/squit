package de.smartsquare.squit

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.typesafe.config.Config
import de.smartsquare.squit.interfaces.SquitJsonPreProcessor

class JsonArrayPreProcessor : SquitJsonPreProcessor {

    override fun process(request: JsonElement?, expectedResponse: JsonElement, config: Config) {
        request?.asJsonArray?.add(JsonPrimitive("pre"))
    }
}
