package io.mozocoin.sdk.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.wallet.ChangePinActivity
import io.mozocoin.sdk.wallet.backup.BackupWalletActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

        button_currencies?.click {
            MessageDialog.show(it.context, R.string.mozo_settings_under_construction)
        }
        button_change_password?.click {
            MessageDialog.show(it.context, R.string.mozo_settings_under_construction)
        }
        button_change_security_pin?.click {
            startActivity(Intent(this, ChangePinActivity::class.java))
        }
        button_backup_wallet?.click {
            startActivity(Intent(this, BackupWalletActivity::class.java))
        }
    }
}
