package com.biglabs.mozo.example.shopper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoWallet
import com.biglabs.mozo.sdk.authentication.AuthenticationListener
import com.biglabs.mozo.sdk.ui.MozoWalletFragment
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

        MozoAuth.getInstance().setAuthenticationListener(object : AuthenticationListener() {
            override fun onChanged(isSinged: Boolean) {
                super.onChanged(isSinged)

                Log.i("MozoSDK", "Authentication changed, signed in: $isSinged")
                Log.i("MozoSDK", "My Mozo address: ${MozoWallet.getInstance().getAddress()}")
            }
        })
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
        }
        return super.onOptionsItemSelected(item)
    }
}
