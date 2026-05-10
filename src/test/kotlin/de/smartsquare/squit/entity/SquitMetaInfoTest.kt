package de.smartsquare.squit.entity

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Instant

class SquitMetaInfoTest {

    @Test
    fun `round-trips through toJson and fromJson correctly`() {
        val original = SquitMetaInfo(Instant.parse("2024-06-15T10:30:45.123000000Z"), 4567L)

        val roundTripped = SquitMetaInfo.fromJson(original.toJson())

        roundTripped shouldBeEqualTo original
    }

    @Test
    fun `fromJson parses the json written by toJson`() {
        val json = """{"date":"2023-11-01T08:00:00Z","duration":99}"""

        val metaInfo = SquitMetaInfo.fromJson(json)

        metaInfo.date shouldBeEqualTo Instant.parse("2023-11-01T08:00:00Z")
        metaInfo.duration shouldBeEqualTo 99L
    }
}
