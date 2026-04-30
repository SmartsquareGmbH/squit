package de.smartsquare.squit.entity

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SquitMetaInfoTest {

    @Test
    fun `round-trips through toJson and fromJson correctly`() {
        val original = SquitMetaInfo(LocalDateTime.of(2024, 6, 15, 10, 30, 45, 123_000_000), 4567L)

        val roundTripped = SquitMetaInfo.fromJson(original.toJson())

        roundTripped shouldBeEqualTo original
    }

    @Test
    fun `fromJson parses the json written by toJson`() {
        val json = """{"date":"2023-11-01T08:00:00","duration":99}"""

        val metaInfo = SquitMetaInfo.fromJson(json)

        metaInfo.date shouldBeEqualTo LocalDateTime.of(2023, 11, 1, 8, 0, 0)
        metaInfo.duration shouldBeEqualTo 99L
    }
}
