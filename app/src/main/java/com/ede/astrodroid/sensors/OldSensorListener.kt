package com.ede.astrodroid.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class OldSensorListener(
    context: Context,
    private val orientation: (Float, Float, Float) -> Unit
) : CommonSensorListener() {

    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.also { orientation ->
            sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME)
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
        when (event?.sensor?.type) {
            Sensor.TYPE_ORIENTATION -> {
                orientation(event.values[0], event.values[1], event.values[2])
            }
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
        //TODO("Not yet implemented")
    }

    override fun onFlushCompleted(event: Sensor?) {
        //TODO("Not yet implemented")
    }
}
