package com.ede.astrodroid.threedee.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.ede.astrodroid.coordinates.JNow
import com.ede.astrodroid.coordinates.Topocentric
import com.ede.astrodroid.location.Latitude
import com.ede.astrodroid.location.Longitude
import com.ede.astrodroid.objects.*
import com.ede.astrodroid.threedee.StarObject
import com.ede.astrodroid.threedee.SolarSystemSpriteObject
import com.ede.astrodroid.threedee.SolarSystemSpritePoint
import com.ede.astrodroid.threedee.DeepSkySpriteObject
import com.ede.astrodroid.threedee.DeepSkySpritePoint
import com.ede.astrodroid.threedee.skyViewMatrix
import com.ede.astrodroid.ui.SkyLabel
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AstronomicalObjectRenderer(
    private val context: Context,
    private val getDeviceToWorld: () -> FloatArray,
    private val getObserverLocation: () -> Pair<Double, Double>?,
    private val getVerticalFieldOfView: () -> Float,
    private val getDisplayOptions: () -> SkyDisplayOptions,
    private val onLabelsChanged: (List<SkyLabel>) -> Unit,
    private val onFrameRendered: (FloatArray) -> Unit,
) : GLSurfaceView.Renderer {
    private val mProjectionMatrix = FloatArray(16)
    private val dataModel by lazy { DataModel(context) }
    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var mStars: StarObject? = null
    private var mDeepSkyObjects: List<DeepSkySpriteObject> = emptyList()
    private lateinit var mSolarSystem: SolarSystemSpriteObject
    private var labelTargets: List<LabelTarget> = emptyList()
    private var lastLatitude = Double.NaN
    private var lastLongitude = Double.NaN
    private var lastLocationCheckMs = 0L
    private var lastSkyBuildMs = 0L
    private var lastLabelUpdateMs = 0L
    private var lastDisplayOptions: SkyDisplayOptions? = null

    private data class LabelTarget(
        val name: String,
        val position: FloatArray,
        val magnitude: Float,
        val priority: Int,
    )

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        updateStarField(force = true)
    }

    private fun updateStarField(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val displayOptions = getDisplayOptions()
        if (!force && now - lastLocationCheckMs < 1000L &&
            displayOptions == lastDisplayOptions) return
        lastLocationCheckMs = now

        // Use Mountain View until Android provides a location fix.
        val (latitude, longitude) = getObserverLocation()
            ?: (37.405f.toDouble() to -122.108f.toDouble())
        if (!force && now - lastSkyBuildMs < 60_000L &&
            kotlin.math.abs(latitude - lastLatitude) < 0.002 &&
            kotlin.math.abs(longitude - lastLongitude) < 0.002 &&
            displayOptions == lastDisplayOptions) return
        lastLatitude = latitude
        lastLongitude = longitude
        lastSkyBuildMs = now

        val observerLatitude = Latitude(latitude)
        val observerLongitude = Longitude(longitude)
        val instant = java.time.Instant.now()
        val topocentric = dataModel.getTopocentricObjects(observerLatitude, observerLongitude).toMutableMap()
        dataModel.getSolarSystemObjects(instant).forEach { body ->
            topocentric[body.id] = Topocentric(body, observerLatitude, observerLongitude)
        }
        val time = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        val observed = topocentric.mapValues { JNow(it.value, time) }

        val starObjects = observed.values.mapNotNull { observedObject ->
            val star = observedObject.getAstronomicalObject()
            if (star.kind != AstronomicalObjectKind.STAR) return@mapNotNull null
            if (star.magnitude > displayOptions.magnitudeLimit) return@mapNotNull null
            star to observedObject.getCartesianCoordinates()
        }
        mStars = if (displayOptions.showStars && starObjects.isNotEmpty()) {
            StarObject(
                starObjects.flatMap { it.second.asIterable() }.toFloatArray(),
                starObjects.flatMap { it.first.color.asIterable() }.toFloatArray()
            )
        } else null

        val deepSkyObjects = observed.values.mapNotNull { observedObject ->
            val deepSkyObject = observedObject.getAstronomicalObject()
            if (deepSkyObject.kind !in setOf(
                    AstronomicalObjectKind.GALAXY,
                    AstronomicalObjectKind.STAR_CLUSTER,
                    AstronomicalObjectKind.NEBULA,
                )) return@mapNotNull null
            if (deepSkyObject.magnitude > displayOptions.magnitudeLimit) return@mapNotNull null
            deepSkyObject to observedObject.getCartesianCoordinates()
        }
        mDeepSkyObjects = if (displayOptions.showGalaxies) {
            deepSkyObjects.groupBy { it.first.kind }.map { (kind, objects) ->
                DeepSkySpriteObject(
                    kind = kind,
                    points = objects.map { (objectData, position) ->
                        DeepSkySpritePoint(position, objectData.color)
                    },
                    baseSize = when (kind) {
                    AstronomicalObjectKind.GALAXY -> 44f
                    AstronomicalObjectKind.STAR_CLUSTER -> 34f
                    AstronomicalObjectKind.NEBULA -> 48f
                    else -> 34f
                    },
                    maxZoomSize = when (kind) {
                    AstronomicalObjectKind.GALAXY -> 200f
                    AstronomicalObjectKind.STAR_CLUSTER -> 150f
                    AstronomicalObjectKind.NEBULA -> 210f
                    else -> 150f
                    }
                )
            }
        } else emptyList()

        labelTargets = observed.values.mapNotNull { observedObject ->
            val astroObject = observedObject.getAstronomicalObject()
            val priority = when (astroObject.kind) {
                AstronomicalObjectKind.SOLAR_SYSTEM -> 3
                AstronomicalObjectKind.GALAXY,
                AstronomicalObjectKind.STAR_CLUSTER,
                AstronomicalObjectKind.NEBULA -> 2
                AstronomicalObjectKind.STAR -> 1
                else -> return@mapNotNull null
            }
            if (astroObject.name.isBlank() || astroObject.name == "??") return@mapNotNull null
            LabelTarget(
                astroObject.name,
                observedObject.getCartesianCoordinates(),
                astroObject.magnitude,
                priority
            )
        }

        val allSolarSprites = observed.values.mapNotNull { observedObject ->
            val body = observedObject.getAstronomicalObject()
            if (body.kind != AstronomicalObjectKind.SOLAR_SYSTEM) return@mapNotNull null
            val visual = body.visual ?: return@mapNotNull null
            SolarSystemSpritePoint(
                body.name, observedObject.getCartesianCoordinates(), visual, body.magnitude
            )
        }
        val solarSprites = if (displayOptions.showPlanets) {
            allSolarSprites.filter { it.magnitude <= displayOptions.magnitudeLimit }
        } else emptyList()
        if (!::mSolarSystem.isInitialized || force) {
            mSolarSystem = SolarSystemSpriteObject(context, allSolarSprites)
        }
        mSolarSystem.update(solarSprites)
        lastDisplayOptions = displayOptions
    }

    override fun onDrawFrame(gl: GL10) {
        updateStarField()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        updateProjectionMatrix()

        val renderedOrientation = getDeviceToWorld()
        val viewMatrix = skyViewMatrix(renderedOrientation)

        val resultMVPMatrix1 = FloatArray(16)
        Matrix.multiplyMM(resultMVPMatrix1, 0, mProjectionMatrix, 0, viewMatrix, 0)

        val fieldOfView = getVerticalFieldOfView().coerceIn(15f, 120f)
        val zoomFraction = ((90f / fieldOfView - 1f) / 5f).coerceIn(0f, 1f)
        mStars?.draw(resultMVPMatrix1)
        mDeepSkyObjects.forEach { it.setZoomFraction(zoomFraction) }
        mDeepSkyObjects.forEach { it.draw(resultMVPMatrix1) }
        mSolarSystem.setZoomFraction(zoomFraction)
        mSolarSystem.draw(resultMVPMatrix1)
        val options = getDisplayOptions()
        if (SystemClock.elapsedRealtime() - lastLabelUpdateMs >= 100L) {
            lastLabelUpdateMs = SystemClock.elapsedRealtime()
            onLabelsChanged(if (options.showNames) projectLabels(resultMVPMatrix1, options) else emptyList())
        }
        onFrameRendered(renderedOrientation)
    }

    private fun projectLabels(mvpMatrix: FloatArray, options: SkyDisplayOptions): List<SkyLabel> {
        val output = mutableListOf<SkyLabel>()
        for (target in labelTargets) {
            val enabled = when (target.priority) {
                3 -> options.showPlanets
                2 -> options.showGalaxies
                else -> options.showStars
            }
            if (!enabled || target.magnitude > options.magnitudeLimit) continue
            val clip = FloatArray(4)
            Matrix.multiplyMV(
                clip, 0, mvpMatrix, 0,
                floatArrayOf(target.position[0], target.position[1], target.position[2], 0f), 0
            )
            if (clip[3] <= 0f || kotlin.math.abs(clip[0]) > clip[3] ||
                kotlin.math.abs(clip[1]) > clip[3]) continue
            val x = (clip[0] / clip[3] + 1f) * 0.5f * surfaceWidth
            val y = (1f - clip[1] / clip[3]) * 0.5f * surfaceHeight
            output += SkyLabel(target.name, x, y, target.priority)
        }
        return output
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        updateProjectionMatrix()
    }

    private fun updateProjectionMatrix() {
        val ratio = surfaceWidth.toFloat() / surfaceHeight
        val near = 0.1f
        val fieldOfView = getVerticalFieldOfView().coerceIn(15f, 120f)
        val halfHeight = near * kotlin.math.tan(Math.toRadians(fieldOfView / 2.0)).toFloat()
        val halfWidth = halfHeight * ratio
        Matrix.frustumM(
            mProjectionMatrix, 0,
            -halfWidth, halfWidth,
            -halfHeight, halfHeight,
            near, 30f
        )
    }

}
