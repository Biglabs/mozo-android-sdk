package io.mozocoin.sdk.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivitySettingsBinding
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.SharedPrefsUtils
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.wallet.ChangePinActivity
import io.mozocoin.sdk.wallet.backup.BackupWalletActivity
import java.util.*
import kotlin.math.max

class SettingsActivity : LocalizationBaseActivity(), SplitInstallStateUpdatedListener {

    private lateinit var binding: ActivitySettingsBinding
    private val splitInstallManager: SplitInstallManager by lazy {
        SplitInstallManagerFactory.create(this)
    }

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
            AlertDialog.Builder(this, R.style.Theme_Material3_Light_Dialog)
                .setTitle(R.string.mozo_languages)
                .setSingleChoiceItems(R.array.languages, lastIndex) { dialog, which ->
                    dialog.dismiss()
                    SharedPrefsUtils.language = locales[which]
                    checkLanguageResource()
                    restartApplication()
                }.create().show()
        }

        binding.buttonDeleteAccount.click {
            if (!MozoAuth.getInstance().isSignedIn()) {
                val prefix = getString(R.string.mozo_view_text_login_require_prefix)
                val btn = getString(R.string.mozo_view_text_login_require_btn)
                val suffix = getString(R.string.mozo_view_text_login_require_suffix)
                Toast.makeText(this, "$prefix $btn $suffix", Toast.LENGTH_LONG).show()
                return@click
            }

            AlertDialog.Builder(this, R.style.DangerousDialog)
                .setTitle(R.string.mozo_dialog_del_account_confirm_title)
                .setMessage(R.string.mozo_dialog_del_account_confirm_msg)
                .setCancelable(false)
                .setNeutralButton(R.string.mozo_button_cancel, null)
                .setNegativeButton(R.string.mozo_delete_account) { d, _ ->
                    d.dismiss()
                    doDeleteAccount()
                }
                .create().show()
        }

        val langs: Set<String> = splitInstallManager.installedLanguages
        Log.i("MozoSDK", langs.joinToString())
    }

    private fun calculateCache() {
        val cacheSize = Support.fileSize(cacheDir)
        val sizeDisplay = Formatter.formatFileSize(this, cacheSize)
        binding.buttonClearCache.text = getString(R.string.mozo_clear_cache, sizeDisplay)
    }

    private fun checkLanguageResource() {
        val request = SplitInstallRequest.newBuilder()
            .addLanguage(SharedPrefsUtils.language)
            .build()
        splitInstallManager.registerListener(this)
        splitInstallManager.startInstall(request)
    }

    private fun restartApplication() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            finishAffinity() // Finishes all activities.
            overridePendingTransition(0, 0)
            startActivity(it)
        }
    }

    private fun doDeleteAccount() {
        MozoAPIsService.getInstance().deleteAccount(this) { _, err ->
            val isSuccess = err.isNullOrEmpty()
            if (isSuccess) {
                MozoAuth.getInstance().signOut(silent = true)
                AlertDialog.Builder(this, R.style.Theme_Material3_Light_Dialog)
                    .setTitle(R.string.mozo_dialog_del_account_remind_title)
                    .setMessage(R.string.mozo_dialog_del_account_remind_msg)
                    .setCancelable(false)
                    .setPositiveButton(R.string.mozo_button_close_app) { d, _ ->
                        d.dismiss()
                        finishAffinity()
                    }
                    .setNegativeButton(R.string.mozo_button_logout_short) { d, _ ->
                        d.dismiss()
                        restartApplication()
                    }
                    .create().show()
            } else {
                MessageDialog.show(this, R.string.error_common)
            }
        }
    }

    override fun onStateUpdate(state: SplitInstallSessionState) {
        Log.i("MozoSDK", "SplitInstall state: ${state.status()}")
        when (state.status()) {
            SplitInstallSessionStatus.INSTALLED -> {}
            else -> {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        splitInstallManager.unregisterListener(this)
    }
}
