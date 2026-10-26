package com.ede.astrodroid.coordinates

abstract class HMSParser(
    hms: String,
    hourMarker: String = "h",
    minuteMarker: String = "m",
    secondMarker: String = "s",
) {
    protected val hourPart: Int
    protected val minutePart: Int
    protected val secondsPart: Double

    init {
        // Parse raw input:
        // 05h 25m 07.86325s
        val parser =
            "^([0-9+-]+)[$hourMarker]? ([0-9.]+)[$minuteMarker]?[ ]?([0-9.]+)?[$secondMarker]?[ ]?(.+)?".toRegex()
        print(parser.matchEntire(hms).toString())
        val matches = parser.matchEntire(hms)
            ?.destructured
            ?.let { (hour, minute, second, rest) ->
                run {
                    parseRest(rest)
                    val minutes: Int
                    val seconds: Double
                    if (second.isEmpty()) {
                        // 05h 40.5m type?
                        val tmp = minute.toDouble()
                        minutes = tmp.toInt()
                        seconds = 60 * (tmp - minutes)
                    } else {
                        // 05h 40m 30s
                        minutes = minute.toInt()
                        seconds = second.toDouble()
                    }
                    Triple(hour.toInt(), minutes, seconds)
                }
            }
            ?: throw IllegalArgumentException("Unparsable Hour Minute Second string '$hms'")
        hourPart = matches.first
        minutePart = matches.second
        secondsPart = matches.third
    }

    open fun parseRest(rest: String) {
    }
}