package com.biglabs.mozo.example.shopper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.authentication.AuthenticationListener

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MozoSDK.initialize(this, MozoSDK.ENVIRONMENT_DEVELOP)

        MozoAuth.getInstance().setAuthenticationListener(object : AuthenticationListener() {
            override fun onChanged(isSinged: Boolean) {
                super.onChanged(isSinged)

                Log.i("MozoSDK", "Authentication changed, signed in: $isSinged")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_demo_fragment -> {
                startActivity(Intent(this, DemoFragmentActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
