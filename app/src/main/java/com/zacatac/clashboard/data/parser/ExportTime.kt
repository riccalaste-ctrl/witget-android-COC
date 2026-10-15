package com.zacatac.clashboard.data.parser

import kotlinx.datetime.Instant

/**
 * Converts the export `timestamp` into an [Instant]. The unit is not documented in the repo:
 * values above 1e11 are treated as milliseconds, smaller ones as seconds (verify on a real export).
 * Missing, non-positive or future values fall back to [fallback] (the import time).
 */
fun exportInstant(timestamp: Long?, fallback: Instant): Instant {
    if (timestamp == null || timestamp <= 0L) return fallback
    val instant = if (timestamp > 100_000_000_000L) {
        Instant.fromEpochMilliseconds(timestamp)
    } else {
        Instant.fromEpochSeconds(timestamp)
    }
    return if (instant > fallback) fallback else instant
}
