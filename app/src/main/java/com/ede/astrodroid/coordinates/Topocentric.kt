package com.ede.astrodroid.coordinates

import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import com.ede.astrodroid.objects.AstronomicalObject

/**
 * Longitude is a (2D) position on a sphere determined by longitudinal lines in other words
 * it determines position around the "equator". Longitude 0 is at prime median (aka. where
 * Greenwich is located)
 *
 * Latitude similarly a position between south and north pole
 * Example: longitude 50N, latitude 30W
 */
data class Topocentric(
    val astroObject: AstronomicalObject,
    val latitude: Latitude,
    val longitude: Longitude
) {
    // TODO: Convert J2000 to topocentric coordinate (somehow - for distant objects....unnecessary)
    val ra: RightAscension = astroObject.location.rightAscension
    val dec: Declination = astroObject.location.declination
}