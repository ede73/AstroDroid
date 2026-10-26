package com.ede.astrodroid.time

/**
 * So surprising Kotlin doesn't have built in time-constants!
 * All constants should be INTs, but due to type casting in Kotlin
 * Integer division yields integer (5/2=2 instead of 2.5).
 * I don't want to type cast everything so declaring these double
 */
const val SECONDS_IN_MINUTE = 60.0
const val MINUTES_IN_HOUR = 60.0
const val SECONDS_IN_HOUR = SECONDS_IN_MINUTE * MINUTES_IN_HOUR
const val HOURS_IN_DAY = 24.0
const val MINUTES_IN_DAY = HOURS_IN_DAY * MINUTES_IN_HOUR
const val SECONDS_IN_DAY = SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY
