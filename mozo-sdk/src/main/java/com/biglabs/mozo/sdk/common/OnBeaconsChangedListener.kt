package com.biglabs.mozo.sdk.common

interface OnBeaconsChangedListener {
    fun onBeaconsChanged(beacons: MutableList<Models.BeaconSignal>?)
}