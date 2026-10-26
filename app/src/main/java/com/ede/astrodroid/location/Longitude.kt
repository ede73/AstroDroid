package com.ede.astrodroid.location

import com.ede.astrodroid.angular.Degree
import com.ede.astrodroid.angular.degreeToDMS
import com.ede.astrodroid.angular.dmsToDegree
import java.util.*
import kotlin.math.abs

data class Longitude(private val position: String) :
    WGS84(position) {
    lateinit var direction: Direction

    // Between 0 and 360(359.9999999999...)
    private val absoluteDegree: Degree

    // always between 0-180 (East) or -180-0 (West)
    val relativeDegree: Degree

    constructor (lon: Double) : this(convertLon(lon))

    init {
        require(abs(hourPart) <= 180) { "Longitude must be 0..180 E/W" }
        require(
            setOf(
                Direction.EAST,
                Direction.WEST
            ).contains(direction)
        ) { "Longitude must have E/W postfix" }
        // +07 24 25.4304W
        absoluteDegree = Degree(dmsToDegree(hourPart, minutePart, secondsPart))
        val multiplier = if (direction == Direction.WEST) -1 else 1
        relativeDegree =
            Degree(multiplier * (if (absoluteDegree.value > 180.0) absoluteDegree.value - 360.0 else absoluteDegree.value))
    }

    override fun parseRest(rest: String) {
        direction = when (rest.uppercase(Locale.getDefault())) {
            "E" -> Direction.EAST
            "W" -> Direction.WEST
            else -> throw IllegalArgumentException("Unknown orientation, only E/W allowed '$rest'")
        }
    }

    override fun toString(): String {
        return convertLon(relativeDegree.value)
    }
}

private fun convertLon(degree: Double): String {
    /**
     * Longitude: -122.083855
     * DMS Long: 122° 5' 1.8780"W
     */
    return degreeToDMS(degree) { degree ->
        if (degree < 0) "W" else "E"
    }
}
