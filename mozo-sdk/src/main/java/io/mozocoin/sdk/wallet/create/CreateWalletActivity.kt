package io.mozocoin.sdk.wallet.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.view.postDelayed
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.databinding.ActivityCreateWalletBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.visible
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

internal class CreateWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateWalletBinding
    private var isInProgress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateWalletBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.buttonCreateAuto.isSelected = true
        binding.buttonCreateAuto.click {
            it.isSelected = true
            binding.buttonCreateManual.isSelected = false
        }

        /**
         * Disable create Wallet manually
         * @see https://vinatechnology.atlassian.net/browse/MOZOX-3
        binding.buttonCreateManual.click {
        it.isSelected = true
        binding.buttonCreateAuto.isSelected = false
        }
         */
        binding.buttonContinue.click {
            showCreatingUI()
            doCreateWallet()
            /**
            when (binding.buttonCreateManual.isSelected) {
            true -> {
            SecurityActivity.start(this, SecurityActivity.KEY_CREATE_PIN, KEY_CREATE_WALLET_MANUAL)

            }
            else -> {
            showCreatingUI()
            doCreateWallet()
            }
            }
             */
        }

        val actionText = getString(R.string.mozo_button_logout_short)
        val spannable =
            SpannableString(getString(R.string.mozo_create_wallet_have_another_account, actionText))
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    EventBus.getDefault().post(MessageEvent.CloseActivities())
                    widget.postDelayed(1000) {
                        MozoAuth.getInstance().signOut()
                    }
                }
            },
            spannable.length - actionText.length,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.createWalletHaveOtherAccountHint.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
        }

        /**
         * Update Oct 2022
         * Skip for display Wallet creation options
         */
        showCreatingUI()
        doCreateWallet()
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
        binding.toolbarMozo.showCloseButton(false)
        binding.createWalletLoading.visible()
    }

    private fun doCreateWallet() = MozoSDK.scope.launch {
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