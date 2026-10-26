package com.ede.astrodroid.coordinates

import com.ede.astrodroid.time.J2000_Julian
import com.ede.astrodroid.time.Julian
import java.time.Instant
import java.util.Locale
import kotlin.math.*

/** Low-precision geocentric lunar position, suitable for a finder view. */
object MoonEphemeris {
    data class Position(
        val rightAscension: RightAscension,
        val declination: Declination,
        val rightAscensionText: String,
        val declinationText: String,
    )

    fun equatorialPosition(instant: Instant): Position {
        val days = Julian(instant).julianDate - J2000_Julian
        val meanLongitude = Math.toRadians((218.316 + 13.176396 * days) % 360.0)
        val meanAnomaly = Math.toRadians((134.963 + 13.064993 * days) % 360.0)
        val latitudeArgument = Math.toRadians((93.272 + 13.229350 * days) % 360.0)

        val longitude = meanLongitude + Math.toRadians(6.289) * sin(meanAnomaly)
        val latitude = Math.toRadians(5.128) * sin(latitudeArgument)
        val obliquity = Math.toRadians(23.439 - 0.00000036 * days)
        val rightAscension = atan2(
            sin(longitude) * cos(obliquity) - tan(latitude) * sin(obliquity),
            cos(longitude)
        ).let { (it + 2.0 * Math.PI) % (2.0 * Math.PI) }
        val declination = asin(
            sin(latitude) * cos(obliquity) +
                cos(latitude) * sin(obliquity) * sin(longitude)
        )

        val hours = Math.toDegrees(rightAscension) / 15.0
        val hour = hours.toInt()
        val minutes = (hours - hour) * 60.0
        val minute = minutes.toInt()
        val seconds = (minutes - minute) * 60.0
        val decDegrees = Math.toDegrees(declination)
        val decAbs = abs(decDegrees)
        val decDegree = decAbs.toInt()
        val decMinutes = (decAbs - decDegree) * 60.0
        val decMinute = decMinutes.toInt()
        val decSeconds = (decMinutes - decMinute) * 60.0

        val raString = String.format(Locale.ROOT, "%02dh %02dm %05.2fs", hour, minute, seconds)
        val sign = if (decDegrees < 0.0) "-" else "+"
        val decString = String.format(
            Locale.ROOT, "%s%02d° %02d' %05.2f\"", sign, decDegree, decMinute, decSeconds
        )
        return Position(
            RightAscension(raString), Declination(decString), raString, decString
        )
    }
}
