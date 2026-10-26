package com.ede.astrodroid.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.util.Log

abstract class CommonSensorListener : SensorEventListener2 {
    abstract fun stop(context: Context)

    protected fun dumpSensors(sensorManager: SensorManager) {
        // lsm6dso LSM6DSO Accelerometer Non-wakeup
        // ak0991x AK09918 Magnetometer Non-wakeup
        // lsm6dso LSM6DSO Gyroscope Non-wakeup
        // gravity  Non-wakeup
        // gravity linear_acceleration Non-wakeup
        // Rotation Vector  Non-wakeup
        // ak0991x AK09918 Magnetometer-Uncalibrated Non-wakeup
        // Game Rotation Vector  Non-wakeup
        // lsm6dso LSM6DSO Gyroscope-Uncalibrated Non-wakeup
        // Motion Sensor
        // Orientation Sensor
        // + MANY MORE
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.e("com.ede.astrodroid", "SENSORS:")
        for (sensor in sensors) {
            Log.e("com.ede.astrodroid", sensor.name + ", " + sensor.stringType)
        }
    }
}