package com.biglabs.mozo.example.shopper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoTodoList
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.authentication.AuthStateListener
import io.mozocoin.sdk.ui.MozoWalletFragment
import io.mozocoin.sdk.ui.SettingsActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        tab_layout?.setupWithViewPager(pager ?: return)
        pager?.adapter = TabsPagerAdapter(supportFragmentManager)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_message -> {
                startActivity(Intent(this, DemoSignMessageActivity::class.java))
            }
            R.id.action_view_profile -> {
                MozoAuth.getInstance().getUserInfo(this, false) {

                }
            }
            R.id.action_view_setting -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_todo -> MozoTodoList.getInstance().open(this)
            R.id.action_address_book -> MozoWallet.getInstance().openAddressBook()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleNotification(intent: Intent?) {
        intent?.let {
            MozoNotification.openDetails(this, it)
        }
    }

    class TabsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val fragments = arrayOf(
                HomeFragment(),
                MozoWalletFragment.getInstance(),
                NotificationFragment()
        )

        private val titles = arrayOf(
                "Home",
                "Wallet",
                "Notify"
        )

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size

        override fun getPageTitle(position: Int): CharSequence {
            return titles[position]
        }
    }
}
