package com.biglabs.mozo.sdk.services

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.biglabs.mozo.sdk.MozoSDK
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials
import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder

class BeaconService private constructor() {

    private val proximityObserver: ProximityObserver? by lazy {
        MozoSDK.context?.let {
            ProximityObserverBuilder(it, EstimoteCloudCredentials(ESTIMOTE_APP_ID, ESTIMOTE_APP_TOKEN))
                    .withAnalyticsReportingDisabled()
                    .onError { throwable ->
                        Toast.makeText(it, "proximityObserver onError:" + throwable.message, Toast.LENGTH_SHORT).show()
                    }
                    .build()
        }
    }
    private var observationHandler: ProximityObserver.Handler? = null

    init {
        MozoSDK.context?.let {
            startRanging(it)
        }
    }

    private fun startRanging(context: Context) {
        Handler().postDelayed({
            Toast.makeText(context, "start Ranging", Toast.LENGTH_SHORT).show()
            val venueZone = ProximityZoneBuilder()
                    .forTag("BiglabsCompany")
                    .inFarRange()
                    .onEnter {
                        Toast.makeText(context, "venueZone onEnter", Toast.LENGTH_SHORT).show()
                    }
                    .onExit {
                        Toast.makeText(context, "venueZone onExit", Toast.LENGTH_SHORT).show()
                    }
                    .onContextChange {
                        Toast.makeText(context, "venueZone onContextChange", Toast.LENGTH_SHORT).show()
                    }
                    .build()
//            observationHandler = proximityObserver.startObserving(venueZone)

        }, 5000)
    }

    fun stopScan() {
        observationHandler?.stop()
    }

    companion object {

        private const val ESTIMOTE_APP_ID = "mozosdk-oqh"
        private const val ESTIMOTE_APP_TOKEN = "76edde3928a11914b0465a82f13ba3cc"

        @Volatile
        private var INSTANCE: BeaconService? = null

        internal fun getInstance(): BeaconService =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: BeaconService()
                }
    }
}