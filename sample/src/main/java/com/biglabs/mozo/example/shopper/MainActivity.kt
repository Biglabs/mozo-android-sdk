package com.biglabs.mozo.example.shopper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.authentication.AuthenticationListener

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
    }
}
