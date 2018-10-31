package com.biglabs.mozo.sdk.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.TextViewCompat
import android.view.View
import android.widget.TextView
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.core.WalletService
import com.biglabs.mozo.sdk.ui.widget.onBackPress
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.view_wallet_backup.*
import kotlinx.android.synthetic.main.view_wallet_security.*
import kotlinx.android.synthetic.main.view_toolbar.view.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class SecurityActivity : BaseActivity() {

    private var mPIN = ""
    private var mPINLength = 0
    private var mShowMessageDuration = 0L
    private var mRequestCode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPINLength = getInteger(R.integer.security_pin_length)
        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration).toLong()

        mRequestCode = intent.getIntExtra(KEY_MODE, mRequestCode)

        when (mRequestCode) {
            KEY_CREATE_PIN -> showBackupUI()
            KEY_ENTER_PIN -> showPinInputRestoreUI()
            KEY_VERIFY_PIN -> showPinVerifyUI()
            KEY_VERIFY_PIN_FOR_SEND -> showPinVerifyUI()
            else -> {
                finishAndRemoveTask()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        input_pin?.hideKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPIN = ""
    }

    private fun showBackupUI() {
        setContentView(R.layout.view_wallet_backup)

        val paddingVertical = resources.dp2Px(10f).toInt()
        val paddingHorizontal = resources.dp2Px(8f).toInt()
        WalletService.getInstance().getSeed()?.split(" ")?.map {
            val word = TextView(this@SecurityActivity)
            word.setPaddingRelative(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
            word.text = it
            TextViewCompat.setTextAppearance(word, R.style.MozoTheme_SeedWords)
            seed_view.addView(word)
        }

        button_stored_confirm.click {
            it.isSelected = !it.isSelected
            button_continue.isEnabled = it.isSelected
        }

        button_continue.click { showPinInputUI() }
    }

    private fun showPinInputUI() {
        setContentView(R.layout.view_wallet_security)

        pin_toolbar.screen_title.setText(R.string.mozo_pin_title)
        sub_title_pin.setText(R.string.mozo_pin_sub_title)
        text_content_pin.setText(R.string.mozo_pin_content)

        input_pin?.apply {
            onBackPress { finishAndRemoveTask() }
            setMaxLength(mPINLength)
            onTextChanged {
                hidePinInputWrongUI()
                it?.toString()?.run {
                    if (this.length < mPINLength) return@run

                    when (mRequestCode) {
                        KEY_CREATE_PIN -> {
                            when {
                                mPIN.isEmpty() -> {
                                    mPIN = this
                                    showPinInputConfirmUI()
                                }
                                mPIN == this -> submitForResult()
                                else -> showPinInputWrongUI()
                            }
                        }
                        else -> submitForResult()
                    }
                }
            }

            GlobalScope.launch(Dispatchers.Main) {
                delay(500L)
                showKeyboard()
            }
        }
        input_pin_checker_status.gone()
    }

    private fun showPinInputRestoreUI() {
        showPinInputUI()
        initRestoreUI()
    }

    private fun showPinVerifyUI() {
        showPinInputUI()
        initVerifyUI(true)
    }

    private fun initRestoreUI(clearPin: Boolean = false) {
        pin_toolbar.screen_title.setText(R.string.mozo_pin_title_restore)
        sub_title_pin.setText(R.string.mozo_pin_sub_title_restore)

        input_pin?.apply {
            visible()
            if (clearPin) {
                text?.clear()
            }
        }
        text_content_pin.visible()
        hideLoadingUI()
    }

    private fun initVerifyUI(clearPin: Boolean = false) {
        initRestoreUI(clearPin)
        if (mRequestCode == KEY_VERIFY_PIN_FOR_SEND) {
            pin_toolbar.screen_title.setText(R.string.mozo_transfer_title)
            sub_title_pin.setText(R.string.mozo_pin_sub_title_send)
        } else {
            pin_toolbar.screen_title.setText(R.string.mozo_pin_title_verify)
            sub_title_pin.setText(R.string.mozo_pin_sub_title)
        }
    }

    private fun showPinInputConfirmUI() {
        sub_title_pin.setText(R.string.mozo_pin_confirm_sub_title)
        text_content_pin.setText(R.string.mozo_pin_confirm_content)

        input_pin.setText("")
        text_content_pin.visible()
        input_pin_checker_status.visible()
        input_pin_checker_status.isSelected = false
    }

    private fun showPinCreatedUI() {
        text_correct_pin.setText(R.string.mozo_pin_msg_create_success)
        text_correct_pin.visible()
        input_pin_checker_status.isSelected = true
        input_pin.visible()
        input_pin.isEnabled = false
        text_content_pin.visible()
        hideLoadingUI()
    }

    private fun showPinInputCorrectUI() {
        showPinCreatedUI()
        text_correct_pin.setText(R.string.mozo_pin_msg_enter_correct)
    }

    private fun showPinInputWrongUI() {
        text_incorrect_pin.visible()
    }

    private fun hidePinInputWrongUI() {
        input_pin_checker_status.isSelected = false
        if (text_incorrect_pin.visibility != View.GONE)
            text_incorrect_pin.gone()
    }

    private fun showLoadingUI() {
        gone(arrayOf(
                text_correct_pin,
                text_incorrect_pin,
                input_pin,
                input_pin_checker_status,
                text_content_pin,
                error_container
        ))

        visible(arrayOf(
                input_loading_indicator,
                input_loading_text
        ))
    }

    private fun hideLoadingUI() {
        gone(arrayOf(
                input_loading_indicator,
                input_loading_text
        ))
    }

    private fun showErrorAndRetryUI() {
        hideLoadingUI()
        error_container.visible()
        button_retry.click {
            submitForResult()
        }
    }

    private fun submitForResult() {
        GlobalScope.launch(Dispatchers.Main) {
            showLoadingUI()

            when (mRequestCode) {
                KEY_CREATE_PIN -> {
                    val isSuccess = WalletService.getInstance()
                            .executeSaveWallet(mPIN, this@SecurityActivity) { submitForResult() }
                            .await()
                    if (!isSuccess) {
                        showErrorAndRetryUI()
                        return@launch
                    }
                    showPinCreatedUI()
                }
                KEY_ENTER_PIN -> {
                    mPIN = input_pin.text.toString()
                    val isCorrect = WalletService.getInstance().validatePin(mPIN).await()
                    initRestoreUI(!isCorrect)
                    if (isCorrect) showPinInputCorrectUI()
                    else {
                        showPinInputWrongUI()
                        return@launch
                    }
                }
                else -> {
                    if (mRequestCode == KEY_VERIFY_PIN || mRequestCode == KEY_VERIFY_PIN_FOR_SEND) {
                        mPIN = input_pin.text.toString()
                        val isCorrect = WalletService.getInstance().validatePin(mPIN).await()
                        initVerifyUI(!isCorrect)
                        if (isCorrect) showPinInputCorrectUI()
                        else {
                            showPinInputWrongUI()
                            return@launch
                        }
                    }
                }
            }
            delay(mShowMessageDuration)

            EventBus.getDefault().post(MessageEvent.Pin(mPIN, mRequestCode))

            val intent = Intent()
            intent.putExtra(KEY_DATA, mPIN)
            setResult(RESULT_OK, intent)
            finishAndRemoveTask()
        }
    }

    companion object {
        private const val KEY_MODE = "KEY_MODE"

        const val KEY_CREATE_PIN = 0x001
        const val KEY_ENTER_PIN = 0x002
        const val KEY_VERIFY_PIN = 0x003
        const val KEY_VERIFY_PIN_FOR_SEND = 0x004

        const val KEY_DATA = "KEY_DATA"

        fun start(activity: Activity, mode: Int, requestCode: Int) {
            Intent(activity, SecurityActivity::class.java).apply {
                putExtra(KEY_MODE, mode)
                activity.startActivityForResult(this, requestCode)
            }
        }
    }
}