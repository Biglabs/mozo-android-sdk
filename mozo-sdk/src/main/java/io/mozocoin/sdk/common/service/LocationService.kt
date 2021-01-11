package io.mozocoin.sdk.common.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.google.android.gms.location.*
import io.mozocoin.sdk.utils.isLocationPermissionGranted
import io.mozocoin.sdk.utils.logAsError

class LocationService(val context: Context) : LocationCallback(), LocationListener {

    private var mListener: ((location: Location) -> Unit)? = null

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val fusedLocationProvider: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    init {
        fetchLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        if (!context.isLocationPermissionGranted()) return

        locationManager.removeUpdates(this)
        fusedLocationProvider.removeLocationUpdates(this)

        fusedLocationProvider.lastLocation.addOnSuccessListener {
            if (it != null) {
                mListener?.invoke(it)

                fusedLocationProvider.removeLocationUpdates(this)
                fusedLocationProvider.requestLocationUpdates(
                        LocationRequest()
                                .setInterval(REQUEST_INTERVAL)
                                .setFastestInterval(REQUEST_INTERVAL)
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                        this,
                        null
                )
            } else fetchNetworkLocation()
        }
        fusedLocationProvider.lastLocation.addOnFailureListener {
            fetchNetworkLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchNetworkLocation() {
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { last ->
            mListener?.invoke(last)
        }
        locationManager.removeUpdates(this)

        val providers = locationManager.allProviders
        val provider = when {
            providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> providers.firstOrNull()
        }
        if (provider.isNullOrEmpty()) return
        try {
            locationManager.requestLocationUpdates(
                    provider,
                    REQUEST_INTERVAL,
                    0f,
                    this
            )
        } catch (ignore: Exception) {
            ignore.printStackTrace()
            "Failed to fetch location".logAsError()
        }
    }

    override fun onLocationResult(result: LocationResult?) {
        /**
         * from FusedLocationProviderClient
         */
        result?.lastLocation?.run {
            mListener?.invoke(this)
        }
    }

    override fun onLocationChanged(location: Location) {
        /**
         * from LocationManager
         */
        location.run {
            mListener?.invoke(this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    fun setListener(listener: (location: Location) -> Unit) {
        mListener = listener
        fetchLocation()
    }

    fun clearListener() {
        mListener = null
        fusedLocationProvider.removeLocationUpdates(this)
        locationManager.removeUpdates(this)
    }

    companion object {

        private const val REQUEST_INTERVAL = 2000L

        fun newInstance(context: Context) = LocationService(context)
    }
}