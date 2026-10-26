package com.ede.astrodroid.coordinates

import com.ede.astrodroid.objects.AstronomicalObject
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * https://ascom-standards.org/Help/Developer/html/72A95B28-BBE2-4C7D-BC03-2D6AB324B6F7.htm
 * J2000 -> Topocentric -> Observed
 * TODO: Not used
 */
class Observed(private val topocentric: Topocentric, private val time: LocalDateTime) {
    // TODO: Somehow convert topocentric coordinates to observed coordinates based on time
    // TODO: Also TODO: For astronomical objects with immense distance, it probably
    // makes no difference where on planet earth (or its orbit) we're viewing them, they
    // are still in roughly in the same place
    // Not sure if there'd be a big difference even with celestial objects
    // JUST to approximate viewing purposes

    fun getAstronomicalObject(): AstronomicalObject {
        return topocentric.astroObject
    }

    fun getCartesianCoordinates(): FloatArray {
        //Stars, Messier objects & Constellations etc
        //These objects have fixed RA and Dec without any time dependence
        // Convert observed spherical coordinates to cartesian coordinates
        val distance = 10.0
        val rRA = topocentric.ra.radian.value
        val rDec = topocentric.dec.radian.value
        val x = distance * cos(rDec) * cos(rRA)
        val y = distance * cos(rDec) * sin(rRA)
        val z = distance * sin(rDec)
        val mag = 1.0
        return floatArrayOf((mag * x).toFloat(), (mag * y).toFloat(), (mag * z).toFloat())
    }
}