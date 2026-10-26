package com.ede.astrodroid.coordinates

import com.ede.astrodroid.time.J2000_Julian
import com.ede.astrodroid.time.Julian
import java.time.Instant
import java.util.Locale
import kotlin.math.*

/** JPL's 1800–2050 low-precision Keplerian planetary positions for a finder view. */
object SolarSystemEphemeris {
    data class Position(
        val name: String,
        val rightAscensionText: String,
        val declinationText: String,
        val magnitude: Float,
    )

    private data class Elements(
        val name: String,
        val a: Double,
        val aRate: Double,
        val e: Double,
        val eRate: Double,
        val inclination: Double,
        val inclinationRate: Double,
        val meanLongitude: Double,
        val meanLongitudeRate: Double,
        val longitudePerihelion: Double,
        val longitudePerihelionRate: Double,
        val longitudeAscendingNode: Double,
        val longitudeAscendingNodeRate: Double,
        val magnitude: Float,
    )

    private data class Vector3(val x: Double, val y: Double, val z: Double) {
        operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
        operator fun unaryMinus() = Vector3(-x, -y, -z)
    }

    private val elements = listOf(
        Elements("Mercury", .38709927, .00000037, .20563593, .00001906, 7.00497902, -.00594749,
            252.25032350, 149472.67411175, 77.45779628, .16047689, 48.33076593, -.12534081, -.6f),
        Elements("Venus", .72333566, .00000390, .00677672, -.00004107, 3.39467605, -.00078890,
            181.97909950, 58517.81538729, 131.60246718, .00268329, 76.67984255, -.27769418, -4.1f),
        Elements("Earth", 1.00000261, .00000562, .01671123, -.00004392, -.00001531, -.01294668,
            100.46457166, 35999.37244981, 102.93768193, .32327364, 0.0, 0.0, -26.7f),
        Elements("Mars", 1.52371034, .00001847, .09339410, .00007882, 1.84969142, -.00813131,
            -4.55343205, 19140.30268499, -23.94362959, .44441088, 49.55953891, -.29257343, -1.5f),
        Elements("Jupiter", 5.20288700, -.00011607, .04838624, -.00013253, 1.30439695, -.00183714,
            34.39644051, 3034.74612775, 14.72847983, .21252668, 100.47390909, .20469106, -2.7f),
        Elements("Saturn", 9.53667594, -.00125060, .05386179, -.00050991, 2.48599187, .00193609,
            49.95424423, 1222.49362201, 92.59887831, -.41897216, 113.66242448, -.28867794, .7f),
        Elements("Uranus", 19.18916464, -.00196176, .04725744, -.00004397, .77263783, -.00242939,
            313.23810451, 428.48202785, 170.95427630, .40805281, 74.01692503, .04240589, 5.7f),
        Elements("Neptune", 30.06992276, .00026291, .00859048, .00005105, 1.77004347, .00035372,
            -55.12002969, 218.45945325, 44.96476227, -.32241464, 131.78422574, -.00508664, 7.8f),
    )

    fun positions(instant: Instant): List<Position> {
        val julianDate = Julian(instant).julianDate
        val centuries = (julianDate - J2000_Julian) / 36525.0
        val earth = heliocentric(elements.first { it.name == "Earth" }, centuries)

        val sunPosition = equatorial(-earth)
        val result = mutableListOf(
            Position("Sun", sunPosition.first, sunPosition.second, -26.7f)
        )
        for (planet in elements.filter { it.name != "Earth" }) {
            val position = equatorial(heliocentric(planet, centuries) - earth)
            result += Position(planet.name, position.first, position.second, planet.magnitude)
        }
        return result
    }

    private fun heliocentric(elements: Elements, centuries: Double): Vector3 {
        val a = elements.a + elements.aRate * centuries
        val eccentricity = elements.e + elements.eRate * centuries
        val inclination = Math.toRadians(elements.inclination + elements.inclinationRate * centuries)
        val longitude = elements.meanLongitude + elements.meanLongitudeRate * centuries
        val perihelion = elements.longitudePerihelion + elements.longitudePerihelionRate * centuries
        val node = elements.longitudeAscendingNode + elements.longitudeAscendingNodeRate * centuries
        val argumentOfPerihelion = Math.toRadians(perihelion - node)
        val meanAnomaly = Math.toRadians(normalizeDegrees(longitude - perihelion))

        var eccentricAnomaly = meanAnomaly + eccentricity * sin(meanAnomaly)
        repeat(8) {
            eccentricAnomaly -= (eccentricAnomaly - eccentricity * sin(eccentricAnomaly) - meanAnomaly) /
                (1.0 - eccentricity * cos(eccentricAnomaly))
        }
        val xOrbit = a * (cos(eccentricAnomaly) - eccentricity)
        val yOrbit = a * sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly)
        val omega = argumentOfPerihelion
        val bigOmega = Math.toRadians(node)
        val cosOmega = cos(bigOmega)
        val sinOmega = sin(bigOmega)
        val cosOmegaSmall = cos(omega)
        val sinOmegaSmall = sin(omega)
        val cosInclination = cos(inclination)
        val sinInclination = sin(inclination)

        return Vector3(
            (cosOmegaSmall * cosOmega - sinOmegaSmall * sinOmega * cosInclination) * xOrbit +
                (-sinOmegaSmall * cosOmega - cosOmegaSmall * sinOmega * cosInclination) * yOrbit,
            (cosOmegaSmall * sinOmega + sinOmegaSmall * cosOmega * cosInclination) * xOrbit +
                (-sinOmegaSmall * sinOmega + cosOmegaSmall * cosOmega * cosInclination) * yOrbit,
            sinOmegaSmall * sinInclination * xOrbit + cosOmegaSmall * sinInclination * yOrbit,
        )
    }

    private fun equatorial(ecliptic: Vector3): Pair<String, String> {
        val obliquity = Math.toRadians(23.43928)
        val x = ecliptic.x
        val y = ecliptic.y * cos(obliquity) - ecliptic.z * sin(obliquity)
        val z = ecliptic.y * sin(obliquity) + ecliptic.z * cos(obliquity)
        val rightAscension = (atan2(y, x) + 2.0 * PI) % (2.0 * PI)
        val declination = atan2(z, sqrt(x * x + y * y))
        val hours = Math.toDegrees(rightAscension) / 15.0
        val hour = hours.toInt()
        val minuteTotal = (hours - hour) * 60.0
        val minute = minuteTotal.toInt()
        val seconds = (minuteTotal - minute) * 60.0
        val decDegrees = Math.toDegrees(declination)
        val decAbs = abs(decDegrees)
        val degree = decAbs.toInt()
        val minuteArcTotal = (decAbs - degree) * 60.0
        val minuteArc = minuteArcTotal.toInt()
        val secondsArc = (minuteArcTotal - minuteArc) * 60.0
        val sign = if (decDegrees < 0.0) "-" else "+"
        return String.format(Locale.ROOT, "%02dh %02dm %05.2fs", hour, minute, seconds) to
            String.format(Locale.ROOT, "%s%02d° %02d' %05.2f\"", sign, degree, minuteArc, secondsArc)
    }

    private fun normalizeDegrees(value: Double): Double = (value % 360.0 + 360.0) % 360.0
}
