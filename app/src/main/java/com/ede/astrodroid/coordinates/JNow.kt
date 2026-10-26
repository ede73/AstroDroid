package com.ede.astrodroid.coordinates

import com.ede.astrodroid.angular.Radian
import com.ede.astrodroid.angular.degreeToRadian
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import com.ede.astrodroid.objects.AstronomicalObject
import com.ede.astrodroid.time.HOURS_IN_DAY
import com.ede.astrodroid.time.J2000_Julian
import com.ede.astrodroid.time.Julian
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.*

private const val JULIAN_DAYS_IN_YEAR = 365.25
private const val JULIAN_DAYS_IN_CENTURY = JULIAN_DAYS_IN_YEAR * 100.0

class JNow(
    private val topocentric: Topocentric,
    private val date: LocalDateTime
) {
    val alt: Radian
    val az: Radian

    init {
//        System.err.println(
//            topocentric.astroObject.location.declination.toString() +
//                    " is " +
//                    topocentric.astroObject.location.rightAscension.toString()
//        )
        val ret = raDecToAltAz(
            topocentric.astroObject.location.rightAscension,
            topocentric.astroObject.location.declination,
            topocentric.latitude,
            topocentric.longitude,
            Julian(date)
        )
        az = Radian(ret[0])
        // Should convert to relative longitudinal degrees ie. -180-180
        // TODO: I'm getting 282 degrees when actually expecting -76
        // 282-360=-76 so it's not so wrong, but some bias shift here
//        System.err.println(ret[1]) // 0.9235471101946278
//        System.err.println(Degree(ret[1])) // 0.9235471101946278
//        System.err.println(Degree(Radian(ret[1])).value) // 52.915351595655736
        alt = Radian(ret[1])
        // TODO:
        //alt = -43f
        // az = 340f
    }

    fun getAstronomicalObject(): AstronomicalObject {
        return topocentric.astroObject
    }

    fun getCartesianCoordinates(): FloatArray {
        val julianNow = Julian(Instant.now())
        val (az, alt, _, _) = raDecToAltAz(
            topocentric.ra, topocentric.dec,
            topocentric.latitude, topocentric.longitude, julianNow
        )
        // Local frame: +X east, +Y north, +Z up. Direct spherical conversion
        // preserves the correct quadrant (tan-based conversion did not).
        val horizontal = cos(alt)
        val skyRadius = 10.0
        return floatArrayOf(
            (skyRadius * horizontal * sin(az)).toFloat(),
            (skyRadius * horizontal * cos(az)).toFloat(),
            (skyRadius * sin(alt)).toFloat()
        )
    }

    fun getCartesianCoordinatesOld(): FloatArray {
        //Stars, Messier objects & Constellations etc
        //These objects have fixed RA and Dec without any time dependence
        // Convert observed spherical coordinates to cartesian coordinates
        val distance = 10.0
        val rRA = topocentric.ra.radian.value
        val rDec = topocentric.dec.radian.value
        val x = distance * cos(rDec) * cos(rRA)
        val y = distance * cos(rDec) * sin(rRA)
        val z = distance * sin(rDec)
        val mag = 1.0
        return floatArrayOf((mag * x).toFloat(), (mag * y).toFloat(), (mag * z).toFloat())
    }

    // This one (modified) from https://www.astrogreg.com/convert_ra_dec_to_alt_az.html
    // Verifies against AstroPy
    private fun raDecToAltAz(
        ra: RightAscension,
        dec: Declination,
        latitude: Latitude,
        longitude: Longitude,
        julian_date_utc: Julian
    ): Array<Double> {
        // Sidereal time is returned in hours; convert to radians before combining
        // it with right ascension (which is already represented in radians).
        val siderealHours = localSiderealTime(julian_date_utc, longitude)
        val localSiderealTime = ((siderealHours % 24.0 + 24.0) % 24.0) * Math.PI / 12.0
        val declinationInRadians = dec.radian.value

        var hourAngle = (localSiderealTime - ra.radian.value)
        if (hourAngle < 0) {
            hourAngle += (2 * Math.PI)
        }
        if (hourAngle > Math.PI) {
            hourAngle -= 2 * Math.PI
        }

        val latitudeInRadians = latitude.relativeDegree.toRadians().value
        var azimuth =
            (atan2(
                sin(hourAngle),
                cos(hourAngle) * sin(latitudeInRadians) - tan(declinationInRadians) * cos(
                    latitudeInRadians
                )
            ))
        val altitude =
            (asin(
                sin(latitudeInRadians) * sin(declinationInRadians) + cos(latitudeInRadians) * cos(
                    declinationInRadians
                ) * cos(
                    hourAngle
                )
            ))
        azimuth += Math.PI
        if (azimuth >= 2.0 * Math.PI) azimuth -= 2.0 * Math.PI

        //if (azimuth < 0) {
        //    azimuth += 2 * Math.PI
        //}
        return arrayOf(azimuth, altitude, localSiderealTime, hourAngle)
    }

    companion object {
        fun localSiderealTime(julian_date_utc: Julian, lon: Longitude): Double {
            //Meeus 13.5 and 13.6, modified so West longitudes are negative and 0 is North
            val gmst = greenwichMeanSiderealTime(julian_date_utc)
            return gmst + lon.relativeDegree.value / (360.0 / 24.0)
        }

        // https://astronomy.stackexchange.com/questions/21002/how-to-find-greenwich-mean-sideral-time
        fun greenwichMeanSiderealTime(julian_date_utc: Julian): Double {
            val midnight = floor(julian_date_utc.julianDate - 0.5) + 0.5
            val hoursSinceMidnight = (julian_date_utc.julianDate - midnight) * HOURS_IN_DAY
            val daysSinceEpoch = julian_date_utc.julianDate - J2000_Julian     // D = 6939.833333
            val centuriesSinceEpoch = daysSinceEpoch / JULIAN_DAYS_IN_CENTURY  // T = 0.190002
            val wholeDaysSinceEpoch = midnight - J2000_Julian    // D0 = 6939.5

            return 6.697374558 + 0.06570982441908 * wholeDaysSinceEpoch + 1.00273790935 * hoursSinceMidnight + 0.000026 * centuriesSinceEpoch.pow(
                2.0
            )        //=470.712605328
        }

        // TODO: Doesn't seem right...
        fun greenwichMeanSiderealTime2(julian_date_utc: Julian): Double {
            //"Expressions for IAU 2000 precession quantities" N. Capitaine1,P.T.Wallace2, and J. Chapront
            val t = ((julian_date_utc.julianDate - J2000_Julian)) / JULIAN_DAYS_IN_CENTURY

            var gmst =
                degreeToRadian(
                    this.earthRotationAngle(julian_date_utc) + (0.014506 + 4612.156534 * t
                            + 1.3915817 * t * t - 0.00000044 * t * t * t
                            - 0.000029956 * t * t * t * t
                            - 0.0000000368 * t * t * t * t * t) / 60.0 / 60.0
                )  //eq 42
            gmst %= 2 * Math.PI
            if (gmst < 0) gmst += (2 * Math.PI)
            return gmst
        }

        private fun earthRotationAngle(julian_date_utc: Julian): Double {
            //IERS Technical Note No. 32

            val t = julian_date_utc.julianDate - J2000_Julian
            val f = julian_date_utc.julianDate % 1.0

            var theta = 2 * Math.PI * (f + 0.7790572732640 + 0.00273781191135448 * t) //eq 14
            theta %= 2 * Math.PI
            if (theta < 0) theta += (2 * Math.PI)

            return theta
        }
    }
}
