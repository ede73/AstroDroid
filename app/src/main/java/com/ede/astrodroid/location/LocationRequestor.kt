package com.ede.astrodroid.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LocationRequestor : LocationListener {
    @Volatile
    var currentLocation: Location? = null
        private set
    private lateinit var locationManager: LocationManager

    fun getLocation(activity: Activity) {
        locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fineGranted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                locationPermissionCode
            )
            return
        }

        val provider = if (fineGranted) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        if (locationManager.isProviderEnabled(provider)) {
            currentLocation = locationManager.getLastKnownLocation(provider)
            locationManager.requestLocationUpdates(provider, 5000, 5f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    companion object {
        const val locationPermissionCode = 666
    }
}
