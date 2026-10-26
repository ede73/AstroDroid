package com.ede.astrodroid

import com.ede.astrodroid.coordinates.RightAscension
import org.junit.Assert
import org.junit.Test

class RightAscensionTest {
    @Test
    fun parseRightAscension1() {
        val candidates = listOf(
            "05h 40m 45.52666s",
            "05h 40m 45.52666",
            "05h 40 45.52666s",
            "05h 40 45.52666",
            "05 40m 45.52666s",
            "05 40m 45.52666",
            "05 40 45.52666s",
            "05 40 45.52666"
        )
        candidates.forEach {
            val ra = RightAscension(it)
            Assert.assertEquals(5, ra.hour)
            Assert.assertEquals(40, ra.minute)
            Assert.assertEquals(45.52666, ra.second, 0.00001)
            Assert.assertEquals(85.18969441666667, ra.degree.value, 0.00001)
        }
    }
}