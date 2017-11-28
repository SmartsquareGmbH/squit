package de.smartsquare.squit.entity

import java.time.LocalDateTime

/**
 * Data class holding further information concerning a single [SquitResult].
 *
 * @property date The date the associated test was executed.
 * @property duration The time the test has taken in milliseconds.
 *
 * @author Ruben Gees
 */
data class SquitMetaInfo(val date: LocalDateTime, val duration: Long)
