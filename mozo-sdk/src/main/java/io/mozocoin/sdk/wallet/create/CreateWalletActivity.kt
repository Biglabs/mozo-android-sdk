package io.mozocoin.sdk.wallet.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.utils.click
import kotlinx.android.synthetic.main.activity_create_wallet.*

internal class CreateWalletActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        button_create_auto?.click {

        }

        button_create_manual?.click {
            SecurityActivity.start(this, SecurityActivity.KEY_CREATE_PIN)
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            Intent(context, CreateWalletActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}