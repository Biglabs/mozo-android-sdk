package io.mozocoin.sdk.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivitySettingsBinding
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.MyContextWrapper
import io.mozocoin.sdk.utils.Support
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

        calculateCache()
        binding.buttonClearCache.click {
            Support.cleanDir(cacheDir)
            calculateCache()
        }

        binding.buttonChangeLanguage.click {
            val context = MyContextWrapper.wrap(this, "ko")
            baseContext.resources.updateConfiguration(
                context.resources.configuration,
                context.resources.displayMetrics
            )
        }
    }

    private fun calculateCache() {
        val cacheSize = Support.fileSize(cacheDir)
        val sizeDisplay = Formatter.formatFileSize(this, cacheSize)
        binding.buttonClearCache.text = getString(R.string.mozo_clear_cache, sizeDisplay)
    }
}
