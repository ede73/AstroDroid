package com.ede.astrodroid

import com.ede.astrodroid.coordinates.Declination
import com.ede.astrodroid.coordinates.J2000
import com.ede.astrodroid.coordinates.RightAscension
import org.junit.Assert
import org.junit.Test

class PolarCartesianTest {

    // https://www.jameswatkins.me/posts/converting-equatorial-to-cartesian.html
    @Test
    fun raDecToCartesianTest() {
        val aldebaran =
            J2000(RightAscension("04h 35m 55.23907s"), Declination("+16° 30′ 33.4885\""))
        Assert.assertEquals(68.9801627931976, aldebaran.rightAscension.degree.value, 0.00001)
        // 4h 35m 55s or 68 58' 49"
        Assert.assertEquals(16.50930236065573, aldebaran.declination.degree.value, 0.00001)
        // dec is also 1h 6m 02s or 0.288142R
    }
}