package io.mozocoin.sdk.wallet.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import io.mozocoin.sdk.MozoAuth
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

    private var isInProgress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        button_create_auto?.isSelected = true
        button_create_auto?.click {
            it.isSelected = true
            button_create_manual?.isSelected = false
        }

        button_create_manual?.click {
            it.isSelected = true
            button_create_auto?.isSelected = false
        }

        button_continue?.click {
            when (button_create_manual?.isSelected) {
                true -> {
                    SecurityActivity.start(this, SecurityActivity.KEY_CREATE_PIN, KEY_CREATE_WALLET_MANUAL)

                }
                else -> {
                    showCreatingUI()
                    doCreateWallet()
                }
            }
        }

        val actionText = getString(R.string.mozo_button_logout_short)
        val spannable = SpannableString(getString(R.string.mozo_create_wallet_have_another_account, actionText))
        spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        EventBus.getDefault().post(MessageEvent.CloseActivities())
                        Handler().postDelayed({ MozoAuth.getInstance().signOut() }, 1000)
                    }
                },
                spannable.length - actionText.length,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        create_wallet_have_other_account_hint?.text = spannable
        create_wallet_have_other_account_hint?.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroy() {
        if (isInProgress) EventBus.getDefault().post(MessageEvent.UserCancel())
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == KEY_CREATE_WALLET_MANUAL) {
            isInProgress = false
            finish()
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
            isInProgress = false
            EventBus.getDefault().post(MessageEvent.CreateWalletAutomatic())
            finish()
        }
    }

    companion object {
        private const val KEY_CREATE_WALLET_MANUAL = 331

        @JvmStatic
        fun start(context: Context) {
            Intent(context, CreateWalletActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}