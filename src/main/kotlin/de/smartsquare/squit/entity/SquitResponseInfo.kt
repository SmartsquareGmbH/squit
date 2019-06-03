package de.smartsquare.squit.entity

import com.google.gson.Gson
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.smartsquare.squit.util.responseCode

/**
 * Data class holding further response information concerning a single [SquitResult].
 *
 * @property responseCode The http status code of the response
 *
 * @author Sascha Koch
 */
data class SquitResponseInfo(val responseCode: String = "") {

    companion object {

        private const val RESPONSECODE = "responseCode"

        /**
         * Constructs a [SquitResponseInfo] instance from the given [json] String.
         */
        fun fromJson(json: String): SquitResponseInfo {
            val config = ConfigFactory.parseString(json)
            return SquitResponseInfo(
                config.getString(RESPONSECODE)
            )
        }
        /**
         * Constructs a [SquitResponseInfo] instance from the given [config] Config.
         */
        fun fromConfig(config: Config): SquitResponseInfo =
            SquitResponseInfo(config.responseCode)

        /**
         * String diff between the SquitResponseInfo objects [me] and [other].
         */
        fun diff(me: SquitResponseInfo, other: SquitResponseInfo): String =
            if (me.responseCode == other.responseCode) {
                ""
            } else {
                "Response differs (response code ${me.responseCode} != ${other.responseCode})."
            }

        /**
         * Returns true if no response code is set.
         */
        fun isDefault(me: SquitResponseInfo): Boolean = me.responseCode.isBlank()
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
