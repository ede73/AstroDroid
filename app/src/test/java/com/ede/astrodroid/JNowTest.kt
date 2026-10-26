package com.ede.astrodroid

import com.ede.astrodroid.coordinates.JNow
import com.ede.astrodroid.coordinates.Topocentric
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import com.ede.astrodroid.objects.DataModel
import com.ede.astrodroid.time.J2000_Julian
import com.ede.astrodroid.time.Julian
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.floor

class JNowTest {
    // Or almost 'JNow' :)
    // Or topocentric coordinate test (and see topocentric tests for now...)

    // Mountain View, CA
    private fun getMountainViewCALocation(): Pair<Latitude, Longitude> {
        return Pair(Latitude("37° 24' 21″N"), Longitude("122° 6' 23″W"))
    }

    private fun getEquatorPrimeMeridianLocation(): Pair<Latitude, Longitude> {
        return Pair(Latitude("0° 0' 0″N"), Longitude("0° 0' 0″E"))
    }

    private fun getJ2000(): LocalDateTime {
        return ZonedDateTime.parse("2000-01-01T12:00:00Z")
            .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    @Test
    fun siderealTimeJ2000() {
        Assert.assertEquals(
            14.752132745009540,
            JNow.greenwichMeanSiderealTime(Julian(J2000_Julian)),
            0.000001
        )
    }

    @Test
    fun siderealTimeOdd() {
        // 2019-01-01 8am
        // https://aa.usno.navy.mil/calculated/siderealtime?date=2019-01-01&time=08%3A00%3A00.000&intv_mag=1.00&intv_unit=1&reps=1&lat=0.0000&lon=0.0000&label=&submit=Get+Data
        // 470%24 = 14h
        // floor(0.712605328 * 60) = 42 minutes
        // 0.712605328 *60 = 45.38 seconds
        val localTime = Julian(2458484.833333333)
        val expected = 470.712605328
        Assert.assertEquals(
            expected,
            JNow.greenwichMeanSiderealTime(localTime),
            0.000001
        )
        val decimalPart = expected - expected.toInt()
        Assert.assertEquals(14 /* hours */, expected.toInt() % 24)
        val fractionalMinutes = decimalPart * 60
        Assert.assertEquals(42 /* minutes */, floor(fractionalMinutes).toInt())
        val reminderSeconds = fractionalMinutes - floor(fractionalMinutes).toInt()
        Assert.assertEquals(45.38 /* seconds */, reminderSeconds * 60, 0.01)
    }

    @Test
    fun siderealLocalAtMinus15() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        // https://aa.usno.navy.mil/calculated/siderealtime?date=2019-01-01&time=08%3A00%3A00.000&intv_mag=1.00&intv_unit=1&reps=1&lat=0.0000&lon=-90&label=&submit=Get+Data
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude("15 0 0W"))
        Assert.assertEquals(
            469.71260532762824, // ok
            localSiderealTime,
            0.000001
        )

        val (hour, minute, seconds) = siderealFractionToHMS(localSiderealTime)
        Assert.assertEquals(13 /* hours */, hour)
        Assert.assertEquals(42 /* minutes */, minute)
        Assert.assertEquals(45.38 /* seconds */, seconds, 0.01)
    }

    @Test
    fun siderealLocalTimeAtGreenwichOdd() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        val gmst = JNow.greenwichMeanSiderealTime(localTime)
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude(0.0))
        // https://aa.usno.navy.mil/calculated/siderealtime?date=2019-01-01&time=08%3A00%3A00.000&intv_mag=1.00&intv_unit=1&reps=1&lat=0.0000&lon=0.0000&label=&submit=Get+Data
        Assert.assertEquals(
            470.712605328,
            gmst,
            0.000001
        )
        // greenwich sidereal mean time = local sidereal mean time (at greenwich)
        Assert.assertEquals(localSiderealTime, gmst, 0.00001)
    }

    @Test
    fun siderealLocalAt15EOrOneHourEast() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        // Should be 15 0 0 E, but seems to require 57 17 45 E to match
        System.err.println(Longitude("0 0 0 E").relativeDegree) // 0.0
        System.err.println(Longitude("0 0 0 W").relativeDegree) // 360.0 (or 0)
        System.err.println(Longitude("15 0 0 E").relativeDegree) // 15.0 (right!)
        System.err.println(Longitude("15 0 0 W").relativeDegree) // 345.0 (should be -15 for this!)
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude("15 0 0E"))
        // https://aa.usno.navy.mil/calculated/siderealtime?date=2019-01-01&time=08%3A00%3A00.000&intv_mag=1.00&intv_unit=1&reps=1&lat=0.0000&lon=0.0000&label=&submit=Get+Data
        Assert.assertEquals(
            471.71260532762824, // ok
            localSiderealTime,
            0.0001
        )

        val (hour, minute, seconds) = siderealFractionToHMS(localSiderealTime)
        Assert.assertEquals(15 /* hours */, hour) // should be 15
        Assert.assertEquals(42 /* minutes */, minute) // should be 42
        Assert.assertEquals(45.38 /* seconds */, seconds, 0.01) // should be 45.3766
    }

    @Test
    fun siderealLocalAt135EOrOneHourEast() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        // Should be 15 0 0 E, but seems to require 57 17 45 E to match
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude("135 0 0E"))
        Assert.assertEquals(
            479.71260532762824, // ok
            localSiderealTime,
            0.0001
        )

        val (hour, minute, seconds) = siderealFractionToHMS(localSiderealTime)
        Assert.assertEquals(23 /* hours */, hour) // should be 15
        Assert.assertEquals(42 /* minutes */, minute) // should be 42
        Assert.assertEquals(45.38 /* seconds */, seconds, 0.01) // should be 45.3766
    }

    @Test
    fun siderealLocalAt150EOrOneHourEast() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        // Should be 15 0 0 E, but seems to require 57 17 45 E to match
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude("150 0 0E"))
        Assert.assertEquals(
            480.71260532762824, // ok
            localSiderealTime,
            0.0001
        )

        val (hour, minute, seconds) = siderealFractionToHMS(localSiderealTime)
        Assert.assertEquals(0 /* hours */, hour) // should be 15
        Assert.assertEquals(42 /* minutes */, minute) // should be 42
        Assert.assertEquals(45.38 /* seconds */, seconds, 0.01) // should be 45.3766
    }

    @Test
    fun siderealLocalAt179() {
        // 2019-01-01 8am
        val localTime = Julian(2458484.833333333)
        // 179 0 0E
        val localSiderealTime = JNow.localSiderealTime(localTime, Longitude(179.0))
        // https://aa.usno.navy.mil/calculated/siderealtime?date=2019-01-01&time=08%3A00%3A00.000&intv_mag=1.00&intv_unit=1&reps=1&lat=0.0000&lon=0.0000&label=&submit=Get+Data
        Assert.assertEquals(
            482.6459386609616, // ok
            localSiderealTime,
            0.0001
        )

        val (hour, minute, seconds) = siderealFractionToHMS(localSiderealTime)
        Assert.assertEquals(2 /* hours */, hour)
        Assert.assertEquals(38 /* minutes */, minute)
        Assert.assertEquals(45.38 /* seconds */, seconds, 0.01)
    }

    @Test
    fun verifyBetelgeuseMountainViewTest() {
        val (lat, lon) = getMountainViewCALocation()
        val betelgeuse =
            DataModel().getTopocentricObjects(lat, lon)["Betelgeuse"]?.astroObject
        // Replace with JNow
        val topocentric = Topocentric(betelgeuse!!, lat, lon)
        val jNow = JNow(topocentric, LocalDateTime.parse("2022-11-28T12:40:00"))

        // according to https://theskylive.com/sky/stars/betelgeuse-alpha-orionis-star
        // in 37° 23’ 09N  122° 05’ 01” W
        //    at 19:21, Betelgeuse Azimuth =  82.6°
        //    at 1:42, Betelgeuse max altitude = 60
        //    at 8:02, Betelgeuse Azimuth =  277.4°

        Assert.assertEquals(-71.08574649448624, jNow.az.toDegrees().value, 0.00001)
        Assert.assertEquals(
            -11.703666244715086,
            jNow.alt.toDegrees().value,
            0.0001
        ) // TODO: maybe -43?

        // In mountain view CA 2022-11-28 12:40:00
        // Betelgeuse sits at -43 degrees
        // And at 340 degrees (near NEE)
        // at J2000 57.26382011, -76.21165082
    }

    @Test
    fun verifyBetelgeuseJ2000() {
        val (lat, lon) = getEquatorPrimeMeridianLocation()
        val betelgeuse =
            DataModel().getTopocentricObjects(lat, lon)["Betelgeuse"]?.astroObject
        // Replace with JNow
        val topocentric = Topocentric(betelgeuse!!, lat, lon)
        val jNow = JNow(topocentric, getJ2000())

        // https://www.astrogreg.com/convert_ra_dec_to_alt_az.html
        // Julian date = 2451545.0
        // lat, lon=0
        // Betelgeuse ra=5.919529, dec=7.407064
        Assert.assertEquals((52.915351595655736), jNow.alt.toDegrees().value, 0.0001)
        // 282-360=-76
        Assert.assertEquals((-77.65529097570996), jNow.az.toDegrees().value, 0.0001)
    }

    private fun siderealFractionToHMS(sidereal: Double): Triple<Int, Int, Double> {
        val hours = (sidereal % 24).toInt()
        val decimalPart = sidereal - sidereal.toInt()
        val fractionalMinutes = decimalPart * 60
        val minutes = floor(fractionalMinutes).toInt()
        val reminderSeconds = (fractionalMinutes - minutes) * 60
        return Triple(hours, minutes, reminderSeconds)
    }
}