package io.mozocoin.sdk.ui.setting

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        button_backup_wallet.click {
            startActivity(Intent(this, BackupWalletActivity::class.java))
        }

    }

}
