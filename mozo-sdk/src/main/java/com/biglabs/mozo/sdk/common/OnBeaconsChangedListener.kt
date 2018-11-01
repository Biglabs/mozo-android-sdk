package com.biglabs.mozo.sdk.common

import com.estimote.coresdk.recognition.packets.Beacon

interface OnBeaconsChangedListener {
    fun onBeaconsChanged(beacons: MutableList<Beacon>?)
}