package de.smartsquare.squit.entity

import com.google.gson.reflect.TypeToken
import de.smartsquare.squit.util.gson
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
            val map = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {})

            return SquitMetaInfo(
                LocalDateTime.parse(map[DATE] as String),
                (map[DURATION] as Number).toLong(),
            )
        }
    }

    /**
     * Converts this instance into a Json representation.
     */
    fun toJson(): String = gson.toJson(
        mapOf(
            DATE to date.toString(),
            DURATION to duration,
        ),
    )
}
