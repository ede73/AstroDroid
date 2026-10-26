package com.ede.astrodroid

import com.ede.astrodroid.coordinates.Topocentric
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import com.ede.astrodroid.objects.DataModel
import org.junit.Assert
import org.junit.Test

class TopocentricTest {
    // Or almost 'JNow' :)
    // Or topocentric coordinate test

    @Test
    fun verifyBetelgeuseTest() {
        // Mountain View, CA
        val lat = Latitude("37° 24' 21″N") // 37.405833
        val lon = Longitude("122° 6' 23″W") // -122.106389
        //val lat = Latitude("37° 23' 09″N")
        //val lon = Longitude("122° 5' 01″W")
        val betelgeuse = DataModel().getTopocentricObjects(lat, lon)["Betelgeuse"]?.astroObject
        val topocentric = Topocentric(betelgeuse!!, lat, lon)

        // according to https://theskylive.com/sky/stars/betelgeuse-alpha-orionis-star
        // in 37° 23’ 09N  122° 05’ 01” W
        //    at 19:21, Betelgeuse Azimuth =  82.6°
        //    at 1:42, Betelgeuse max altitude = 60
        //    at 8:02, Betelgeuse Azimuth =  277.4°

        // WRONG!
        // WRONG!
        Assert.assertEquals(88.79291666667, topocentric.ra.degree.value, 0.0001)
        Assert.assertEquals(7.406944444445, topocentric.dec.degree.value, 0.0001)

        // IN 37° 24' 24.3″N 122° 6' 23″W (at 0 meters)
        // Also 2022-11-26 12h37m55s aka. universal time 2022-11-26T20:37:55 JD=2459910.35966
        // Hour angle:  10h56m04.5s
        // Azimuth: 338°10'12"
        // Geometric altitude: -42°45'27.1"
        // Mean of the date RA: 05h56m24.753s DE:+07°24'34.11"
        // Astrometric J2000 RA: 05h55m10.348s DE:+07°24'25.69"
        // Ecliptic  L: +89°04'36" B:-16°01'24"
        // Galactic  L: +199°47'14" B:-08°57'30

        // In mountain view CA 2022-11-28 12:40:00
        // Betelgeuse sits at -43 degrees
        // And at 340 degrees (near NEE)
    }
}