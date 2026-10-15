package com.zacatac.clashboard.data.parser

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportTimeTest {

    private val importTime = Instant.fromEpochSeconds(1_800_000_000)

    @Test
    fun missingOrNonPositiveTimestampFallsBackToImportTime() {
        assertEquals(importTime, exportInstant(null, importTime))
        assertEquals(importTime, exportInstant(0L, importTime))
    }

    @Test
    fun secondsTimestampIsUsed() {
        assertEquals(Instant.fromEpochSeconds(1_799_999_000), exportInstant(1_799_999_000L, importTime))
    }

    @Test
    fun millisecondsTimestampIsUsed() {
        assertEquals(Instant.fromEpochMilliseconds(1_799_999_000_123L), exportInstant(1_799_999_000_123L, importTime))
    }

    @Test
    fun futureTimestampFallsBackToImportTime() {
        assertEquals(importTime, exportInstant(1_900_000_000L, importTime))
    }
}
