package io.mozocoin.sdk.wallet.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.visible
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

internal class CreateWalletActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        button_create_auto?.click {
            showCreatingUI()
            doCreateWallet()
        }

        button_create_manual?.click {
            SecurityActivity.start(this, SecurityActivity.KEY_CREATE_PIN)
        }
    }

    override fun onBackPressed() {
        /* Prevent back press */
        //super.onBackPressed()
    }

    private fun showCreatingUI() {
        toolbar_mozo?.showCloseButton(false)
        create_wallet_loading?.visible()
    }

    private fun doCreateWallet() = GlobalScope.launch {
        MozoWallet.getInstance().getWallet(true)?.encrypt()
        MozoWallet.getInstance().executeSaveWallet(this@CreateWalletActivity, null) {
            EventBus.getDefault().post(MessageEvent.CreateWalletAutomatic())
            finish()
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