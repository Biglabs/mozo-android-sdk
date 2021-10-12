package com.biglabs.mozo.example.shopper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.biglabs.mozo.example.shopper.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import io.mozocoin.sdk.*
import io.mozocoin.sdk.authentication.AuthStateListener
import io.mozocoin.sdk.ui.MozoWalletFragment
import io.mozocoin.sdk.ui.SettingsActivity
import io.mozocoin.sdk.utils.adjustFontScale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tabsPagerAdapter: TabsPagerAdapter by lazy { TabsPagerAdapter(this) }

    override fun onAttachedToWindow() {
        adjustFontScale()
        super.onAttachedToWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.pager.adapter = tabsPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabsPagerAdapter.getPageTitle(position)
        }.attach()

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
            R.id.action_maintenance -> {
                MozoSDK.startMaintenanceMode(this)
            }
            R.id.action_view_setting -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_todo -> MozoTodoList.getInstance().open(this)
            R.id.action_address_book -> MozoWallet.getInstance().openAddressBook()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleNotification(intent: Intent?) {
        intent?.let {
            MozoNotification.handleAction(this, it)
        }
    }

    class TabsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        private val fragments = arrayOf(
                HomeFragment(),
                MozoWalletFragment.getInstance(),
                NotificationFragment()
        )

        private val titles = arrayOf(
                "Home",
                "Wallet",
                "Notification"
        )

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]

        fun getPageTitle(position: Int): String = titles[position]
    }
}
