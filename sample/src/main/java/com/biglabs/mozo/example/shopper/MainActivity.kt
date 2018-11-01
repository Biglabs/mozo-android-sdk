package com.biglabs.mozo.example.shopper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoBeacon
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.authentication.AuthenticationListener
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.OnBeaconsChangedListener
import com.biglabs.mozo.sdk.utils.logAsError

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MozoSDK.initialize(this)

        MozoAuth.getInstance().setAuthenticationListener(object : AuthenticationListener() {
            override fun onChanged(isSinged: Boolean) {
                super.onChanged(isSinged)

                Log.i("MozoSDK", "Authentication changed, signed in: $isSinged")
            }
        })

        MozoBeacon.getInstance().onBeaconsChangedListener = object : OnBeaconsChangedListener {
            override fun onBeaconsChanged(beacons: MutableList<Models.BeaconSignal>?) {
                "beacons: $beacons\n\n".logAsError("onBeaconsChanged")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MozoBeacon.getInstance().startRanging(this)
    }
}
