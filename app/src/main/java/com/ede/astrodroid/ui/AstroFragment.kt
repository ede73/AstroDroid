package com.ede.astrodroid.ui

import android.os.Bundle
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import android.hardware.GeomagneticField
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ede.astrodroid.MainActivity
import com.ede.astrodroid.databinding.FragmentAstroBinding
import com.ede.astrodroid.objects.SkyDisplayOptions
import com.ede.astrodroid.sensors.NewSensorListener
import com.ede.astrodroid.threedee.renderer.AstronomicalObjectRenderer


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 *  C:\users\ede\appdata\local\android\sdk\platform-tools\adb.exe connect 192.168.1.51:37635
 */
class AstroFragment : Fragment() {

    private companion object {
        const val ORIENTATION_STATS_INTERVAL_MS = 100L
        const val ORIENTATION_RENDER_THRESHOLD = 0.0015f
    }

    private var _binding: FragmentAstroBinding? = null
    private val binding get() = _binding!!
    private lateinit var sensors: NewSensorListener
    @Volatile private var verticalFieldOfView = 90f
    @Volatile private var displayOptions = SkyDisplayOptions()
    @Volatile private var deviceToWorld = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
    private var lastStatsUpdateMs = 0L
    private val framePendingOrRendering = AtomicBoolean(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAstroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.glStarView.setEGLContextClientVersion(3) // TODO: 3?

        //        binding.buttonFirst.setOnClickListener {
        //            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        //        }

        // Set the Renderer for drawing on the GLSurfaceView
        val labelOverlay = binding.skyLabelOverlay
        binding.glStarView.setRenderer(AstronomicalObjectRenderer(
            context = requireContext().applicationContext,
            getDeviceToWorld = { deviceToWorld },
            getObserverLocation = { (activity as? MainActivity)?.currentObserverLocation() },
            getVerticalFieldOfView = { verticalFieldOfView },
            getDisplayOptions = { displayOptions },
            onLabelsChanged = { labels -> labelOverlay.setSkyLabels(labels) },
            onFrameRendered = { orientation -> onFrameRendered(orientation) }
        ))
        binding.glStarView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        sensors = NewSensorListener(requireContext()) { matrix, azimuth, pitch, roll ->
            val corrected = rotateMagneticNorthToTrueNorth(matrix)
            deviceToWorld = corrected
            val now = SystemClock.elapsedRealtime()
            if (now - lastStatsUpdateMs >= ORIENTATION_STATS_INTERVAL_MS) {
                lastStatsUpdateMs = now
                updateOrientation(azimuth, pitch, roll)
            }
            requestLatestFrame()
        }
        requestLatestFrame()

        installDisplayControls()
        installSkyGestures()
        updateViewAngle()
    }

    private fun installDisplayControls() {
        binding.checkStars.setOnCheckedChangeListener { _, checked ->
            displayOptions = displayOptions.copy(showStars = checked)
            requestLatestFrame()
        }
        binding.checkPlanets.setOnCheckedChangeListener { _, checked ->
            displayOptions = displayOptions.copy(showPlanets = checked)
            requestLatestFrame()
        }
        binding.checkGalaxies.setOnCheckedChangeListener { _, checked ->
            displayOptions = displayOptions.copy(showGalaxies = checked)
            requestLatestFrame()
        }
        binding.checkNames.setOnCheckedChangeListener { _, checked ->
            displayOptions = displayOptions.copy(showNames = checked)
            requestLatestFrame()
            if (!checked) binding.skyLabelOverlay.setSkyLabels(emptyList())
        }
        binding.seekMagnitude.max = 120
        binding.seekMagnitude.progress = 120
        binding.seekMagnitude.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val limit = -2f + progress / 10f
                displayOptions = displayOptions.copy(magnitudeLimit = limit)
                binding.textMagnitudeLimit.text = if (limit >= 10f) {
                    "Magnitude: all"
                } else {
                    String.format("Magnitude ≤ %.1f", limit)
                }
                binding.glStarView.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })
        binding.textMagnitudeLimit.text = "Magnitude: all"
    }

    private fun installSkyGestures() {
        val scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    verticalFieldOfView =
                        (verticalFieldOfView / detector.scaleFactor).coerceIn(15f, 120f)
                    updateViewAngle()
                    requestLatestFrame()
                    return true
                }
            }
        )
        val swipeDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onFling(
                    start: MotionEvent?, end: MotionEvent, velocityX: Float, velocityY: Float
                ): Boolean {
                    val startY = start?.y ?: return false
                    val deltaY = end.y - startY
                    if (kotlin.math.abs(deltaY) < 80f || kotlin.math.abs(velocityY) < 100f) {
                        return false
                    }
                    binding.debugTexts.visibility =
                        if (deltaY < 0f) View.GONE else View.VISIBLE
                    return true
                }
            }
        )
        binding.glStarView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            swipeDetector.onTouchEvent(event)
            true
        }
    }

    // Calculate and update the current view in form of x * y degrees
    private fun updateViewAngle() {
        binding.textViewAngularView.text = String.format(
            "Field of view %.0f° · pinch to zoom · swipe up to hide stats",
            verticalFieldOfView
        )
    }

    private fun updateOrientation(heading: Float, pitch: Float, roll: Float) {
        binding.tvPhoneHeading.text = String.format("Heading: %f", heading)
        binding.tvPhonePitch.text = String.format("Pitch: %f", pitch)
        binding.tvPhoneRoll.text = String.format("Roll: %f", roll)
    }

    private fun requestLatestFrame() {
        if (framePendingOrRendering.compareAndSet(false, true)) {
            binding.glStarView.requestRender()
        }
    }

    private fun onFrameRendered(renderedOrientation: FloatArray) {
        framePendingOrRendering.set(false)
        val latest = deviceToWorld
        val moved = renderedOrientation.indices.any {
            kotlin.math.abs(latest[it] - renderedOrientation[it]) >= ORIENTATION_RENDER_THRESHOLD
        }
        if (moved) requestLatestFrame()
    }

    private fun rotateMagneticNorthToTrueNorth(matrix: FloatArray): FloatArray {
        val (latitude, longitude) =
            (activity as? MainActivity)?.currentObserverLocation()
                ?: (37.405 to -122.108)
        val declination = Math.toRadians(
            GeomagneticField(
                latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()
            ).declination.toDouble()
        ).toFloat()
        val sin = kotlin.math.sin(declination)
        val cos = kotlin.math.cos(declination)
        val corrected = matrix.copyOf()
        for (column in 0..2) {
            val magneticEast = matrix[column]
            val magneticNorth = matrix[3 + column]
            corrected[column] = cos * magneticEast + sin * magneticNorth
            corrected[3 + column] = -sin * magneticEast + cos * magneticNorth
        }
        return corrected
    }

    override fun onDestroyView() {
        if (::sensors.isInitialized) sensors.stop(requireContext())
        super.onDestroyView()
        _binding = null
    }
}
