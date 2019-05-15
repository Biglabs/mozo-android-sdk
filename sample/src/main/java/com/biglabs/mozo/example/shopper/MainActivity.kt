package com.biglabs.mozo.example.shopper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.authentication.AuthStateListener
import io.mozocoin.sdk.ui.MozoWalletFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabhost?.apply {
            setup(this@MainActivity, supportFragmentManager, android.R.id.tabcontent)
            addTab(newTabSpec("home").setIndicator("Home"), HomeFragment::class.java, null)
            addTab(newTabSpec("wallet").setIndicator("Wallet"), MozoWalletFragment::class.java, null)
            addTab(newTabSpec("notification").setIndicator("Notification"), NotificationFragment::class.java, null)
        }

        MozoAuth.getInstance().addAuthStateListener(object : AuthStateListener() {

            override fun onSignedIn() {
                super.onSignedIn()
                Log.i("MozoSDK", "onSignedIn")
            }

            override fun onAuthStateChanged(singedIn: Boolean) {

                Log.i("MozoSDK", "Authentication changed, signed in: $singedIn")
                Log.i("MozoSDK", "My Mozo address: ${MozoWallet.getInstance().getAddress()}")
            }

            override fun onAuthCanceled() {
                Toast.makeText(this@MainActivity, "User canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthFailed() {
                Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        handleNotification(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotification(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_sign_message -> {
                startActivity(Intent(this, DemoSignMessageActivity::class.java))
            }
            R.id.action_view_profile -> {
                MozoAuth.getInstance().getUserInfo(this, false) {

                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleNotification(intent: Intent?) {
        intent?.let {
            MozoNotification.openDetails(this, it)
        }
    }
}
