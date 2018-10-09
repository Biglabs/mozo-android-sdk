package com.biglabs.mozo.example.shopper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.auth.AuthenticationListener

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MozoSDK.getInstance().auth.setAuthenticationListener(object : AuthenticationListener() {
            override fun onChanged(isSinged: Boolean) {
                super.onChanged(isSinged)
            }
        })
    }
}
