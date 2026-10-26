package com.ede.astrodroid.objects

import android.content.Context
import com.ede.astrodroid.R
import com.ede.astrodroid.coordinates.J2000
import com.ede.astrodroid.coordinates.MoonEphemeris
import com.ede.astrodroid.coordinates.SolarSystemEphemeris
import com.ede.astrodroid.coordinates.Topocentric
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import org.json.JSONObject
import java.time.Instant
import java.util.Locale
import kotlin.math.abs

enum class AstronomicalObjectKind {
    STAR, GALAXY, STAR_CLUSTER, NEBULA, SOLAR_SYSTEM, OTHER
}

/** One catalog record for any named or catalogued sky object. */
data class AstronomicalObject(
    val id: String,
    val name: String,
    val identifiers: List<String>,
    val location: J2000,
    val magnitude: Float,
    val kind: AstronomicalObjectKind,
    val color: FloatArray = floatArrayOf(1f, 0.95f, 0.8f, 1f),
    val visual: SkyVisual? = null,
    val dynamicPosition: String? = null,
)

/** Optional sky-view sprite. Atlas cell is row-major in a 4x4 texture atlas. */
data class SkyVisual(
    val atlasResource: Int,
    val atlasCell: Int,
    val pointSizePx: Float,
    val pointSizeAtMaxZoomPx: Float = pointSizePx,
) {
    init {
        require(atlasCell in 0..15)
        require(pointSizePx > 0f)
        require(pointSizeAtMaxZoomPx >= pointSizePx)
    }
}

/** Loads the compact, source-attributed JSON export bundled in assets. */
class DataModel(context: Context) {
    private val objects: List<AstronomicalObject> = loadObjects(context)

    fun getTopocentricObjects(
        latitude: Latitude,
        longitude: Longitude,
    ): Map<String, Topocentric> = objects
        .asSequence()
        .filter { it.kind != AstronomicalObjectKind.SOLAR_SYSTEM }
        .associate { objectData ->
            objectData.id to Topocentric(objectData, latitude, longitude)
        }

    /** Dynamic bodies use the same generic object records, with their positions updated per frame. */
    fun getSolarSystemObjects(instant: Instant): List<AstronomicalObject> {
        val positions = SolarSystemEphemeris.positions(instant).associateBy { it.name }
        val moon = MoonEphemeris.equatorialPosition(instant)
        return objects.asSequence()
            .filter { it.kind == AstronomicalObjectKind.SOLAR_SYSTEM }
            .mapNotNull { objectData ->
                if (objectData.dynamicPosition == "moon") {
                    objectData.copy(
                        location = J2000(moon.rightAscensionText, moon.declinationText),
                        magnitude = -12.7f,
                    )
                } else {
                    val position = positions[objectData.name] ?: return@mapNotNull null
                    objectData.copy(
                        location = J2000(position.rightAscensionText, position.declinationText),
                        magnitude = position.magnitude,
                    )
                }
            }.toList()
    }

    private fun loadObjects(context: Context): List<AstronomicalObject> {
        val json = context.assets.open("sky_catalog.json").bufferedReader().use { it.readText() }
        val rows = JSONObject(json).getJSONArray("objects")
        return buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val ra = row.getDouble("raDeg")
                val dec = row.getDouble("decDeg")
                val visualJson = row.optJSONObject("visual")
                val visual = visualJson?.let {
                    val resource = when (it.optString("atlas")) {
                        "celestial_atlas" -> R.drawable.celestial_atlas
                        else -> error("Unknown sky sprite atlas: ${it.optString("atlas")}")
                    }
                    SkyVisual(
                        resource,
                        it.getInt("cell"),
                        it.getDouble("sizePx").toFloat(),
                        it.getDouble("maxZoomSizePx").toFloat(),
                    )
                }
                val identifiersJson = row.getJSONArray("identifiers")
                val identifiers = buildList(identifiersJson.length()) {
                    for (identifierIndex in 0 until identifiersJson.length()) {
                        add(identifiersJson.getString(identifierIndex))
                    }
                }
                val colorJson = row.optJSONArray("color")
                val color = if (colorJson == null) {
                    floatArrayOf(1f, 0.95f, 0.8f, 1f)
                } else FloatArray(colorJson.length()) { colorJson.getDouble(it).toFloat() }
                add(
                    AstronomicalObject(
                        id = row.getString("id"),
                        name = row.optString("name"),
                        identifiers = identifiers,
                        location = J2000(raDegreesToHours(ra), decDegreesToDms(dec)),
                        magnitude = row.optDouble("magnitude", 99.0).toFloat(),
                        kind = AstronomicalObjectKind.valueOf(row.getString("kind")),
                        color = color,
                        visual = visual,
                        dynamicPosition = row.optString("dynamicPosition").takeIf(String::isNotBlank),
                    )
                )
            }
        }
    }

    private fun raDegreesToHours(degrees: Double): String {
        val totalSeconds = ((degrees % 360.0 + 360.0) % 360.0) / 15.0 * 3600.0
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds - hours * 3600) / 60).toInt()
        val seconds = totalSeconds - hours * 3600 - minutes * 60
        return String.format(Locale.ROOT, "%02dh %02dm %05.2fs", hours, minutes, seconds)
    }

    private fun decDegreesToDms(degrees: Double): String {
        val absolute = abs(degrees)
        val wholeDegrees = absolute.toInt()
        val totalMinutes = (absolute - wholeDegrees) * 60.0
        val minutes = totalMinutes.toInt()
        val seconds = (totalMinutes - minutes) * 60.0
        val sign = if (degrees < 0) "-" else "+"
        return String.format(Locale.ROOT, "%s%02d° %02d' %05.2f\"", sign, wholeDegrees, minutes, seconds)
    }
}
