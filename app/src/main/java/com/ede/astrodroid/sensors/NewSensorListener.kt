package com.ede.astrodroid.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

// Prefer Android's fused rotation vector (gyro + accelerometer + magnetometer)
// over rebuilding orientation from two independently sampled sensors.
class NewSensorListener(
    context: Context,
    private val orientation: (FloatArray, Float, Float, Float) -> Unit,
) : CommonSensorListener() {
    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { rotationVector ->
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stop(context: Context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val angles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, angles)
            var azimuth = Math.toDegrees(angles[0].toDouble()).toFloat() % 360f
            if (azimuth < 0f) azimuth += 360f
            // Contract with the renderers is degrees: azimuth, pitch, roll.
            orientation(
                rotationMatrix.copyOf(),
                azimuth,
                Math.toDegrees(angles[1].toDouble()).toFloat(),
                Math.toDegrees(angles[2].toDouble()).toFloat()
            )
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
        //TODO("Not yet implemented")
    }

    override fun onFlushCompleted(event: Sensor?) {
        //TODO("Not yet implemented")
    }
}
