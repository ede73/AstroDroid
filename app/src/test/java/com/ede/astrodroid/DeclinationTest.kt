package com.ede.astrodroid

import com.ede.astrodroid.coordinates.Declination
import org.junit.Assert
import org.junit.Test

class DeclinationTest {
    @Test
    fun parseDeclination() {
        val candidates = listOf(
            "+07° 24′ 25.4304\"",
            "+07° 24′ 25.4304",
            "+07° 24 25.4304\"",
            "+07° 24 25.4304",
            "+07 24′ 25.4304\"",
            "+07 24′ 25.4304″",
            "+07 24 25.4304\"",
            "+07 24 25.4304",

            "+07° 24' 25.4304\"",
            "+07° 24' 25.4304",
            "+07 24' 25.4304\"",
            "+07 24' 25.4304"
        )

        candidates.forEach {
            val declination = Declination(it)
            Assert.assertEquals(7, declination.arcHour)
            Assert.assertEquals(24, declination.arcMinute)
            Assert.assertEquals(25.4304, declination.arcSecond, 0.0001)
            Assert.assertEquals(7.407064, declination.degree.value, 0.00001)
        }
    }
}