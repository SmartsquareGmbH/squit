package de.smartsquare.squit.entity

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.typesafe.config.Config
import de.smartsquare.squit.config.expectedResponseCode

/**
 * Data class holding further response information concerning a single [SquitResult].
 *
 * @property responseCode The http status code of the response.
 */
data class SquitResponseInfo(val responseCode: Int = 0) {

    companion object {
        private const val RESPONSE_CODE = "responseCode"

        /**
         * Constructs a [SquitResponseInfo] instance from the given [json] String.
         */
        fun fromJson(json: String): SquitResponseInfo =
            Gson().fromJson(json, SquitResponseInfo::class.java)

        /**
         * Constructs a [SquitResponseInfo] instance from the given [config] Config.
         */
        fun fromConfig(config: Config): SquitResponseInfo =
            SquitResponseInfo(config.expectedResponseCode)
    }

    /**
     * Converts this instance into a Json representation.
     */
    fun toJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(
        mapOf(
            RESPONSE_CODE to responseCode,
        ),
    )

    /**
     * String diff between the this and [other].
     */
    fun diff(other: SquitResponseInfo): String = if (this.responseCode == other.responseCode) {
        ""
    } else {
        "Response differs (response code ${this.responseCode} != ${other.responseCode})."
    }

    /**
     * Returns true if responseCode is default.
     */
    val isDefault = responseCode == 0
}
