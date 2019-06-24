package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.activity_change_pin.*
import kotlinx.android.synthetic.main.fragment_reset_enter_pin.*
import kotlinx.android.synthetic.main.view_message_progress_status.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChangePinActivity : BaseActivity() {

    private var mInputStep = STEP_INPUT_CURRENT
    private var lastInputPin: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_loading)

        if (!MozoAuth.getInstance().isSignedIn()) {
            MozoAuth.getInstance().isSignUpCompleted(this) { isCompleted ->
                if (isCompleted) loadWallet()
                else {
                    val prefix = getString(R.string.mozo_view_text_login_require_prefix)
                    val btn = getString(R.string.mozo_view_text_login_require_btn)
                    val suffix = getString(R.string.mozo_view_text_login_require_suffix)
                    Toast.makeText(this, "$prefix $btn $suffix", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            return
        }

        loadWallet()
    }

    override fun onBackPressed() {
        if (change_pin_loading_view?.isVisible == true) return
        super.onBackPressed()
    }

    private fun loadWallet() {
        MozoAuth.getInstance().syncProfile(this) {
            if (!it) {
                "sync profile failed".logAsError()
                finish()
                return@syncProfile
            }

            val wallet = MozoWallet.getInstance().getWallet()
            if (wallet == null) {
                "wallet null".logAsError()
                // TODO OMG current wallet is null ?
                finish()
                return@syncProfile
            }

            setContentView(R.layout.activity_change_pin)
            initUI()
            if (wallet.isUnlocked()) {
                showPinInputNewUI()
            } else showPinInputUI()
        }
    }

    private fun initUI() {
        reset_pin_enter_pin_input?.apply {
            onBackPress { finish() }
            onTextChanged {
                text_incorrect_pin?.gone()
                if (it?.length != reset_pin_enter_pin_input?.getMaxLength()) return@onTextChanged

                when (mInputStep) {
                    STEP_INPUT_CURRENT -> {
                        lastInputPin = it
                        showLoadingUI()
                        verifyCurrentPin { isOk ->
                            if (isOk) showPinInputNewUI()
                            else showErrorMsg()
                        }
                    }
                    STEP_INPUT_NEW -> {
                        lastInputPin = it
                        showPinInputConfirmUI()
                    }
                    STEP_INPUT_CONFIRM -> {
                        if (lastInputPin?.toString()?.equals(it?.toString()) == true)
                            doChangePin()
                        else
                            showErrorMsg()
                    }
                }
            }
        }
    }

    private fun showPinInputUI() {
        mInputStep = STEP_INPUT_CURRENT
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_change_sub_enter_current)
        reset_pin_enter_pin_input?.text = null
        reset_pin_enter_pin_input?.focus()
    }

    private fun showPinInputNewUI() {
        mInputStep = STEP_INPUT_NEW
        lastInputPin = null
        hideLoadingUI()
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_reset_header_create)
        reset_pin_enter_pin_input?.text = null
        reset_pin_enter_pin_input?.focus()
    }

    private fun showPinInputConfirmUI() {
        mInputStep = STEP_INPUT_CONFIRM
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_change_sub_confirm_new)
        reset_pin_enter_pin_sub_content?.setText(R.string.mozo_pin_change_confirm_content)
        reset_pin_enter_pin_input?.text = null
        reset_pin_enter_pin_input?.focus()
    }

    private fun showLoadingUI() {
        text_correct_pin?.gone()
        text_incorrect_pin?.gone()
        reset_pin_enter_pin_input?.gone()
        reset_pin_progress?.visible()
    }

    private fun hideLoadingUI() {
        reset_pin_enter_pin_input?.visible()
        reset_pin_progress?.gone()
    }

    private fun showErrorMsg() {
        hideLoadingUI()
        reset_pin_enter_pin_input?.text = null
        reset_pin_enter_pin_input?.focus()
        text_incorrect_pin?.visible()
    }

    private fun showChangePinFailedUI() {
        change_pin_toolbar?.showCloseButton(true)
        change_pin_message_view?.apply {
            this.view_message_icon?.setImageResource(R.drawable.ic_error_general)
            this.view_message_title?.setText(R.string.mozo_dialog_error_msg)
            this.view_message_retry_btn?.setText(R.string.mozo_button_try_again)
            this.view_message_retry_btn?.click {
                doChangePin()
            }
            visible()
        }
    }

    private fun showChangePinSuccessUI() {
        change_pin_message_view?.apply {
            this.view_message_icon?.setImageResource(R.drawable.ic_check_green)
            this.view_message_title?.setText(R.string.mozo_pin_change_success)
            this.view_message_retry_btn?.setText(R.string.mozo_button_done)
            this.view_message_retry_btn?.click {
                setResult(RESULT_OK)
                finish()
            }
            visible()
        }
    }

    private fun verifyCurrentPin(callback: (Boolean) -> Unit) = GlobalScope.launch {
        if (lastInputPin.isNullOrEmpty()) {
            withContext(Dispatchers.Main) { callback.invoke(false) }
        }
        val wallet = MozoWallet.getInstance().getWallet()?.decrypt(lastInputPin!!.toString())
        withContext(Dispatchers.Main) {
            callback.invoke(wallet?.isUnlocked() == true)
        }
    }

    private fun doChangePin() {
        lastInputPin ?: return
        change_pin_toolbar?.showCloseButton(false)
        change_pin_loading_view?.visible()
        reset_pin_enter_pin_input?.clearFocus()
        reset_pin_enter_pin_input?.hideKeyboard()

        GlobalScope.launch {
            val wallet = MozoWallet.getInstance().getWallet()
            if (wallet?.isUnlocked() == true) {
                wallet.changePin(lastInputPin.toString())

                withContext(Dispatchers.Main) {
                    MozoWallet.getInstance().executeSaveWallet(this@ChangePinActivity, null) {
                        val profile = MozoSDK.getInstance().profileViewModel.getProfile()
                        if (it && profile != null) {
                            profile.walletInfo = wallet.buildWalletInfo()
                            MozoAuth.getInstance().saveUserInfo(this@ChangePinActivity, profile, wallet) { isSaveSuccess ->
                                if (isSaveSuccess) {
                                    MozoWallet.getInstance().getWallet()?.lock()
                                    showChangePinSuccessUI()
                                } else showChangePinFailedUI()
                            }

                        } else showChangePinFailedUI()
                    }
                }
            } else showChangePinFailedUI()
        }
    }

    companion object {
        private const val STEP_INPUT_CURRENT = 1
        private const val STEP_INPUT_NEW = 2
        private const val STEP_INPUT_CONFIRM = 3
    }
}