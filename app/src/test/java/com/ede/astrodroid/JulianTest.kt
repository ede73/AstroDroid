package com.ede.astrodroid

import com.ede.astrodroid.time.J2000_Julian
import com.ede.astrodroid.time.Julian
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class JulianTest {
    private fun getJ2000(): LocalDateTime {
        return ZonedDateTime.parse("2000-01-01T12:00:00Z")
            .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    @Test
    fun julianJ2000Test() {
        val julian = Julian(getJ2000())
        Assert.assertEquals(J2000_Julian, julian.julianDate, 0.01)
    }

    @Test
    fun julianJ2022Nov28Test() {
        val julian = Julian(LocalDateTime.parse("2022-11-28T12:00:00"))
        Assert.assertEquals(2459912.0, julian.julianDate, 0.01)
    }
}