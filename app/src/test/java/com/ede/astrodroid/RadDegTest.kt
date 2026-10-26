package com.ede.astrodroid

import com.ede.astrodroid.angular.Degree
import com.ede.astrodroid.angular.Radian
import org.junit.Assert
import org.junit.Test

class RadDegTest {
    @Test
    fun degreesToRadians() {
        val deg = Degree(75.0)
        Assert.assertEquals(1.309, deg.toRadians().value, 0.001)
    }

    @Test
    fun radiansToDegrees() {
        val rad = Radian(1.309)
        Assert.assertEquals(75.0, rad.toDegrees().value, 0.001)
    }
}