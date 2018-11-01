package com.biglabs.mozo.sdk

import android.app.Activity
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.OnBeaconsChangedListener
import com.estimote.coresdk.common.requirements.SystemRequirementsChecker
import com.estimote.coresdk.observation.region.beacon.BeaconRegion
import com.estimote.coresdk.recognition.packets.Beacon
import com.estimote.coresdk.service.BeaconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MozoBeacon private constructor() {

    var onBeaconsChangedListener: OnBeaconsChangedListener? = null

    private val beaconManager: BeaconManager by lazy { BeaconManager(MozoSDK.context!!.applicationContext) }
    private val beaconRegion: BeaconRegion by lazy { BeaconRegion("mozo-store", null, null, null) }

    fun startRanging(activity: Activity) {
        if (SystemRequirementsChecker.checkWithDefaultDialogs(activity) && MozoAuth.getInstance().isSignedIn()) {
            beaconManager.setRangingListener { _: BeaconRegion?, beacons: MutableList<Beacon>? ->
                val beaconSignalList = mutableListOf<Models.BeaconSignal>()
                beacons?.map {
                    beaconSignalList.add(Models.BeaconSignal(it))
                }
                onBeaconsChangedListener?.onBeaconsChanged(beaconSignalList)
            }
            beaconManager.connect {
                beaconManager.startRanging(beaconRegion)
            }
        }
    }

    fun stopRanging() = GlobalScope.launch(Dispatchers.Main) {
        beaconManager.stopRanging(beaconRegion)
        beaconManager.disconnect()
    }

    companion object {

        private const val ESTIMOTE_APP_ID = "mozosdk-oqh"
        private const val ESTIMOTE_APP_TOKEN = "76edde3928a11914b0465a82f13ba3cc"

        @Volatile
        private var instance: MozoBeacon? = null

        fun getInstance(): MozoBeacon = instance ?: synchronized(this) {
            instance = MozoBeacon()
            return@synchronized instance!!
        }
    }
}