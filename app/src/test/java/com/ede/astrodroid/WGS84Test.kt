package com.ede.astrodroid

import com.ede.astrodroid.location.Direction
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import org.junit.Assert
import org.junit.Test

class WGS84Test {
    private val candidates = listOf(
        "+07° 24′ 25.4304\"",
        "+07° 24′ 25.4304",
        "+07° 24 25.4304\"",
        "+07° 24 25.4304",
        "+07 24′ 25.4304\"",
        "+07 24′ 25.4304",
        "+07 24 25.4304\"",
        "+07 24 25.4304",
    )

    @Test
    fun longitudeTest() {
        listOf("E", "W").forEach { direction ->
            candidates.forEach {
                System.err.println("parse $it$direction")
                val lon = Longitude(it + direction)
                if (direction == "E") {
                    Assert.assertEquals(7.407064, lon.relativeDegree.value, 0.00001)
                } else {
                    // TODO:
                    System.err.println(lon.relativeDegree)
                    System.err.println(lon)
                    System.err.println(it)
                    Assert.assertEquals(-7.407064, lon.relativeDegree.value, 0.00001)
                }
                Assert.assertEquals(
                    stringDirectionToDirection(direction), lon.direction
                )
            }
        }
    }

    @Test
    fun directLongitudeTest() {
        Assert.assertEquals(1.0, Longitude(1.0).relativeDegree.value, 0.01)
        Assert.assertEquals(179.0, Longitude(179.0).relativeDegree.value, 0.01)
        //Assert.assertEquals(-179.0, Longitude(181.0).absoluteDegree.degree, 0.01)
        Assert.assertEquals(-179.0, Longitude("179 0 0W").relativeDegree.value, 0.01)
        Assert.assertEquals(-1.0, Longitude("1 0 0W").relativeDegree.value, 0.01)
    }

    @Test
    fun directLatitudeTest() {
        Assert.assertEquals(1.0, Latitude(1.0).relativeDegree.value, 0.01)
        Assert.assertEquals(79.0, Latitude(79.0).relativeDegree.value, 0.01)
        //Assert.assertEquals(-179.0, Longitude(181.0).absoluteDegree.degree, 0.01)
        Assert.assertEquals(-79.0, Latitude("79 0 0S").relativeDegree.value, 0.01)
        Assert.assertEquals(-1.0, Latitude("1 0 0S").relativeDegree.value, 0.01)
    }

    @Test
    fun latitudeTest() {
        listOf("N", "S").forEach { direction ->
            candidates.forEach {
                val lon = Latitude(it + direction)
                if (direction == "N")
                    Assert.assertEquals(7.407064, lon.relativeDegree.value, 0.00001)
                else
                    Assert.assertEquals(-7.407064, lon.relativeDegree.value, 0.00001)
                Assert.assertEquals(
                    stringDirectionToDirection(direction), lon.direction
                )
            }
        }
    }

    @Test
    fun latitudeWrongDirectionTest() {
        listOf("N", "S").forEach { direction ->
            try {
                Latitude("+07° 24′ 25.4304\"$direction")
            } catch (_e: IllegalArgumentException) {
                assert(true)
            } catch (_e: Exception) {
                assert(false)
            }
        }
    }

    @Test
    fun longitudeWrongDirectionTest() {
        listOf("E", "W").forEach { direction ->
            try {
                Longitude("+07° 24′ 25.4304\"$direction")
            } catch (_e: IllegalArgumentException) {
                assert(true)
            } catch (_e: Exception) {
                assert(false)
            }
        }
    }

    private fun stringDirectionToDirection(direction: String): Direction {
        return when (direction) {
            "N" -> Direction.NORTH
            "E" -> Direction.EAST
            "S" -> Direction.SOUTH
            "W" -> Direction.WEST
            else -> {
                throw java.lang.IllegalArgumentException()
            }
        }
    }
}