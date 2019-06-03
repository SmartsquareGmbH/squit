package de.smartsquare.squit.entity

import com.google.gson.Gson
import com.typesafe.config.ConfigFactory

/**
 * Data class holding further response information concerning a single [SquitResult].
 *
 * @property responseCode The http status code of the response
 *
 * @author Sascha Koch
 */
data class SquitResponseInfo(val responseCode: Int) {

    companion object {

        private const val RESPONSECODE = "responseCode"

        /**
         * Constructs a [SquitResponseInfo] instance from the given [json] String.
         */
        fun fromJson(json: String): SquitResponseInfo {
            val config = ConfigFactory.parseString(json)
            return SquitResponseInfo(
                config.getInt(RESPONSECODE)
            )
        }
    }

    /**
     * Converts this instance into a Json representation.
     */
    fun toJson(): String = Gson().toJson(
        mapOf(
            RESPONSECODE to responseCode
        )
    )
}
