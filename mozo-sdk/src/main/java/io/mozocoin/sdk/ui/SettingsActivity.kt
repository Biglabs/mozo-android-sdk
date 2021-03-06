package io.mozocoin.sdk.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivitySettingsBinding
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.wallet.ChangePinActivity
import io.mozocoin.sdk.wallet.backup.BackupWalletActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

        binding.buttonCurrencies.click {
            MessageDialog.show(it.context, R.string.mozo_settings_under_construction)
        }
        binding.buttonChangePassword.click {
            MessageDialog.show(it.context, R.string.mozo_settings_under_construction)
        }
        binding.buttonChangeSecurityPin.click {
            startActivity(Intent(this, ChangePinActivity::class.java))
        }
        binding.buttonBackupWallet.click {
            startActivity(Intent(this, BackupWalletActivity::class.java))
        }
    }
}
