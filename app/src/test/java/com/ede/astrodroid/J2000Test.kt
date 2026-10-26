package com.ede.astrodroid

import com.ede.astrodroid.coordinates.Declination
import com.ede.astrodroid.coordinates.J2000
import com.ede.astrodroid.coordinates.RightAscension
import org.junit.Assert
import org.junit.Test

class J2000Test {
    @Test
    fun makeJ2000Test() {
        val j2000 = J2000(
            RightAscension("05h 40m 45.52666s"),
            Declination("+07° 24′ 25.4304\"")
        )
        Assert.assertEquals(5, j2000.rightAscension.hour)
        Assert.assertEquals(40, j2000.rightAscension.minute)
        Assert.assertEquals(45.52666, j2000.rightAscension.second, 0.0001)
        Assert.assertEquals(85.18969441666667, j2000.rightAscension.degree.value, 0.00001)

        Assert.assertEquals(7, j2000.declination.arcHour)
        Assert.assertEquals(24, j2000.declination.arcMinute)
        Assert.assertEquals(25.4304, j2000.declination.arcSecond, 0.001)
        Assert.assertEquals(7.407064, j2000.declination.degree.value, 0.00001)
    }

    @Test
    fun makeJ2000Test2() {
        val j2000 = J2000(
            RightAscension("05h 40.5m"),
            Declination("+07° 24′")
        )
        Assert.assertEquals(5, j2000.rightAscension.hour)
        Assert.assertEquals(40, j2000.rightAscension.minute)
        Assert.assertEquals(30.0, j2000.rightAscension.second, 0.0001)
        Assert.assertEquals(85.125, j2000.rightAscension.degree.value, 0.001)

        Assert.assertEquals(7, j2000.declination.arcHour)
        Assert.assertEquals(24, j2000.declination.arcMinute)
        Assert.assertEquals(0.0, j2000.declination.arcSecond, 0.001)
        Assert.assertEquals(7.4, j2000.declination.degree.value, 0.1)
    }
}