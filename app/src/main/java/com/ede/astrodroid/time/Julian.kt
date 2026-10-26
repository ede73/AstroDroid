package com.ede.astrodroid.time

import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.floor

const val J2000_Julian = 2451545.0
private const val Days_Between_UnixEpoch_J2000 = 10957
//Period.between(LocalDate.of(2000, 1, 1), LocalDate.of(1970, 1, 1) ).days

data class Julian(val julianDate: Double) {
    constructor(date: LocalDateTime) : this(localDateToJulian(date))
    constructor(instant: Instant) : this(
        2440587.5 + instant.epochSecond / SECONDS_IN_DAY + instant.nano / (SECONDS_IN_DAY * 1_000_000_000.0)
    )

    init {
        require(Days_Between_UnixEpoch_J2000 == 10957)
    }

    override fun toString(): String {
        return "$julianDate"
    }
}

private fun daysSinceUnixEpoch(date: LocalDateTime): Int {
    return floor(date.toEpochSecond(ZoneOffset.UTC) / SECONDS_IN_DAY).toInt()
}

private fun localDateToJulian(date: LocalDateTime): Double {
    return J2000_Julian - Days_Between_UnixEpoch_J2000 + daysSinceUnixEpoch(date) + floor(date.hour / HOURS_IN_DAY) + floor(
        date.minute / MINUTES_IN_DAY
    ) + date.second / SECONDS_IN_DAY
}

