package de.smartsquare.squit.entity

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import java.time.LocalDateTime

/**
 * Data class holding further information concerning a single [SquitResult].
 *
 * @property date The date the associated test was executed.
 * @property duration The time the test has taken in milliseconds.
 *
 * @author Ruben Gees
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
                    config.getLong(DURATION)
            )
        }
    }

    /**
     * Converts this instance into a Json representation.
     */
    fun toJson(): String = ConfigValueFactory.fromMap(mapOf(
            DATE to date.toString(),
            DURATION to duration)
    ).render(ConfigRenderOptions.concise())
}
