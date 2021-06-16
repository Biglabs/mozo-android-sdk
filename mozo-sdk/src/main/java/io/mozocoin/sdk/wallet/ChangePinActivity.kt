package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivityChangePinBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*

internal class ChangePinActivity : BaseActivity() {

    private lateinit var binding: ActivityChangePinBinding
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

        MainScope().launch {
            delay(500)
            loadWallet()
        }
    }

    override fun onBackPressed() {
        if (binding.changePinLoadingView.isVisible) return
        super.onBackPressed()
    }

    private fun loadWallet() {
        MozoAuth.getInstance().syncProfile(this) {
            if (!it) {
                "sync profile failed".logAsError()
                finish()
                return@syncProfile
            }

            MozoWallet.getInstance().getWallet { wallet ->
                if (wallet == null) {
                    "wallet null".logAsError()
                    // TODO OMG current wallet is null ?
                    finish()
                    return@getWallet
                }

                initUI()
                if (wallet.isUnlocked()) {
                    showPinInputNewUI()
                } else showPinInputUI()
            }
        }
    }

    private fun initUI() {
        binding = ActivityChangePinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fragmentResetEnterPin.resetPinEnterPinInput.apply {
            onBackPress { finish() }
            onTextChanged {
                binding.fragmentResetEnterPin.textIncorrectPin.gone()
                if (it?.length != getMaxLength()) return@onTextChanged

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
                        if (lastInputPin?.toString()?.equals(it.toString()) == true)
                            doChangePin()
                        else
                            showErrorMsg()
                    }
                }
            }
        }
    }

    private fun showPinInputUI() = MainScope().launch {
        delay(200)
        mInputStep = STEP_INPUT_CURRENT
        binding.fragmentResetEnterPin.resetPinEnterPinHeader.setText(R.string.mozo_pin_change_sub_enter_current)
        binding.fragmentResetEnterPin.resetPinEnterPinInput.text = null
        binding.fragmentResetEnterPin.resetPinEnterPinInput.focus()
    }

    private fun showPinInputNewUI() = MainScope().launch {
        delay(200)
        mInputStep = STEP_INPUT_NEW
        lastInputPin = null
        hideLoadingUI()
        binding.fragmentResetEnterPin.resetPinEnterPinHeader.setText(R.string.mozo_pin_reset_header_create)
        binding.fragmentResetEnterPin.resetPinEnterPinInput.text = null
        binding.fragmentResetEnterPin.resetPinEnterPinInput.focus()
    }

    private fun showPinInputConfirmUI() = MainScope().launch {
        delay(200)
        mInputStep = STEP_INPUT_CONFIRM
        binding.fragmentResetEnterPin.resetPinEnterPinHeader.setText(R.string.mozo_pin_change_sub_confirm_new)
        binding.fragmentResetEnterPin.resetPinEnterPinSubContent.setText(R.string.mozo_pin_change_confirm_content)
        binding.fragmentResetEnterPin.resetPinEnterPinInput.text = null
        binding.fragmentResetEnterPin.resetPinEnterPinInput.focus()
    }

    private fun showLoadingUI() {
        binding.fragmentResetEnterPin.textCorrectPin.gone()
        binding.fragmentResetEnterPin.textIncorrectPin.gone()
        binding.fragmentResetEnterPin.resetPinEnterPinInput.gone()
        binding.fragmentResetEnterPin.resetPinProgress.visible()
    }

    private fun hideLoadingUI() {
        binding.fragmentResetEnterPin.resetPinEnterPinInput.visible()
        binding.fragmentResetEnterPin.resetPinProgress.gone()
    }

    private fun showErrorMsg() = MainScope().launch {
        delay(200)
        hideLoadingUI()
        binding.fragmentResetEnterPin.resetPinEnterPinInput.text = null
        binding.fragmentResetEnterPin.resetPinEnterPinInput.focus()
        binding.fragmentResetEnterPin.textIncorrectPin.visible()
    }

    private fun showChangePinFailedUI() {
        binding.changePinToolbar.showCloseButton(true)
        binding.changePinMessageView.apply {
            this.viewMessageIcon.setImageResource(R.drawable.ic_error_general)
            this.viewMessageTitle.setText(R.string.mozo_dialog_error_msg)
            this.viewMessageRetryBtn.setText(R.string.mozo_button_try_again)
            this.viewMessageRetryBtn.click {
                doChangePin()
            }
            this.root.visible()
        }
    }

    private fun showChangePinSuccessUI() {
        binding.changePinMessageView.apply {
            this.viewMessageIcon.setImageResource(R.drawable.ic_check_green)
            this.viewMessageTitle.setText(R.string.mozo_pin_change_success)
            this.viewMessageRetryBtn.setText(R.string.mozo_button_done)
            this.viewMessageRetryBtn.click {
                setResult(RESULT_OK)
                finish()
            }
            this.root.visible()
        }
    }

    private fun verifyCurrentPin(callback: (Boolean) -> Unit) = MozoSDK.scope.launch {
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
        binding.changePinToolbar.showCloseButton(false)
        binding.changePinLoadingView.visible()
        binding.fragmentResetEnterPin.resetPinEnterPinInput.clearFocus()
        binding.fragmentResetEnterPin.resetPinEnterPinInput.hideKeyboard()

        MozoSDK.scope.launch {
            val wallet = MozoWallet.getInstance().getWallet()
            if (wallet?.isUnlocked() == true) {
                wallet.changePin(lastInputPin.toString())

                fun callback(success: Boolean) {
                    val profile = MozoSDK.getInstance().profileViewModel.getProfile()
                    if (success && profile != null) {
                        profile.walletInfo = wallet.buildWalletInfo()
                        MozoAuth.getInstance().saveUserInfo(this@ChangePinActivity, profile, wallet) { isSaveSuccess ->
                            if (isSaveSuccess) {
                                MozoWallet.getInstance().getWallet()?.lock()
                                showChangePinSuccessUI()
                            } else showChangePinFailedUI()
                        }

                    } else showChangePinFailedUI()
                }

                if (wallet.isAutoWallet) {
                    MozoWallet.getInstance().syncWalletAutoToPin(
                            wallet.buildWalletInfo(),
                            this@ChangePinActivity,
                            ::callback
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        MozoWallet.getInstance()
                                .executeSaveWallet(this@ChangePinActivity, null, ::callback)
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