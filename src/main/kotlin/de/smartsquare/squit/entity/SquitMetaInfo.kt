package de.smartsquare.squit.entity

import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import java.time.LocalDateTime

/**
 * Data class holding further information concerning a single [SquitResult].
 *
 * @property date The date the associated test was executed.
 * @property duration The time the test has taken in milliseconds.
 */
data class SquitMetaInfo(val date: LocalDateTime, val duration: Long) {

    companion object {

        private const val DATE = "date"
        private const val DURATION = "duration"

        /**
         * Constructs a [SquitMetaInfo] instance from the given [json] String.
         */
        fun fromJson(json: String): SquitMetaInfo {
            val config = ConfigFactory.parseString(json)

            return SquitMetaInfo(
                LocalDateTime.parse(config.getString(DATE)),
                config.getLong(DURATION),
            )
        }
    }

    /**
     * Converts this instance into a Json representation.
     */
    fun toJson(): String = Gson().toJson(
        mapOf(
            DATE to date.toString(),
            DURATION to duration,
        ),
    )
}
