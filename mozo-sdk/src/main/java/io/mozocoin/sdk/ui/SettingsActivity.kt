package io.mozocoin.sdk.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivitySettingsBinding
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.SharedPrefsUtils
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.logAsError
import io.mozocoin.sdk.wallet.ChangePinActivity
import io.mozocoin.sdk.wallet.backup.BackupWalletActivity
import java.util.*
import kotlin.math.max


class SettingsActivity : LocalizationBaseActivity() {

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
            val lastLocale: Locale = SharedPrefsUtils.language
            val locales = arrayOf(Locale.US, Locale.KOREA, Locale("vi", "VN"))
            val lastIndex = max(locales.indexOf(lastLocale), 0)
            AlertDialog.Builder(this)
                .setTitle(R.string.mozo_languages)
                .setSingleChoiceItems(R.array.languages, lastIndex) { dialog, which ->
                    dialog.dismiss()
                    SharedPrefsUtils.language = locales[which]
                    restartApplication()
                }.create().show()
        }

        val splitInstallManager = SplitInstallManagerFactory.create(this)
        val langs: Set<String> = splitInstallManager.installedLanguages
        langs.joinToString().logAsError("vuu")
    }

    private fun calculateCache() {
        val cacheSize = Support.fileSize(cacheDir)
        val sizeDisplay = Formatter.formatFileSize(this, cacheSize)
        binding.buttonClearCache.text = getString(R.string.mozo_clear_cache, sizeDisplay)
    }

    private fun restartApplication() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            finishAffinity() // Finishes all activities.
            overridePendingTransition(0, 0)
            startActivity(it)
        }
    }
}
