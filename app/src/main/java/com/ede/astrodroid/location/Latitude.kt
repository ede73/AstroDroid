package com.ede.astrodroid.location

import com.ede.astrodroid.angular.Degree
import com.ede.astrodroid.angular.degreeToDMS
import com.ede.astrodroid.angular.dmsToDegree
import java.util.*
import kotlin.math.abs

data class Latitude(private val position: String) :
    WGS84(position) {
    lateinit var direction: Direction

    // 0 at equator, 90 at north pole, -90 at south pole
    val relativeDegree: Degree

    constructor (lat: Double) : this(convertLat(lat))

    init {
        require(abs(hourPart) <= 90) { "Latitude must be -90..0..90 N/S" }
        require(
            setOf(
                Direction.NORTH,
                Direction.SOUTH
            ).contains(direction)
        ) { "Latitude must have N/S postfix" }
        val multiplier = if (direction == Direction.SOUTH) -1 else 1
        relativeDegree =
            Degree(multiplier * dmsToDegree(hourPart, minutePart, secondsPart))
    }

    override fun parseRest(rest: String) {
        direction = when (rest.uppercase(Locale.getDefault())) {
            "N" -> Direction.NORTH
            "S" -> Direction.SOUTH
            else -> throw IllegalArgumentException("Unknown orientation, only N/S allowed '$rest'")
        }
    }

    override fun toString(): String {
        return convertLat(relativeDegree.value)
    }
}

private fun convertLat(degree: Double): String {
    /**
     * Latitude: 37.386051
     * DMS Lat: 37° 23' 9.7836"N
     */
    return degreeToDMS(degree) { degree ->
        if (degree < 0) "S" else "N"
    }
}
