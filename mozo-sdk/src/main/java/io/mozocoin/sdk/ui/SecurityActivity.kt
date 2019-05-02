package io.mozocoin.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.ui.widget.onBackPress
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.view_toolbar.view.*
import kotlinx.android.synthetic.main.view_wallet_backup.*
import kotlinx.android.synthetic.main.view_wallet_security.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class SecurityActivity : BaseActivity() {

    private var mPIN = ""
    private var mPINLength = 0
    private var mShowMessageDuration = 0L
    private var mRequestCode = -1
    private var willReturnsResult = false

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

    override fun onResume() {
        super.onResume()
        willReturnsResult = false
    }

    override fun onStop() {
        super.onStop()
        input_pin?.hideKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPIN = ""
        if (!willReturnsResult) {
            EventBus.getDefault().post(MessageEvent.UserCancel())
        }
    }

    private fun showBackupUI() {
        setContentView(R.layout.view_wallet_backup)

        val paddingVertical = resources.dp2Px(10f).toInt()
        val paddingHorizontal = resources.dp2Px(8f).toInt()
        MozoWallet.getInstance().getWallet(true)?.mnemonicPhrases()?.map {
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
                                mPIN == this -> {
                                    input_pin_checker_status.isSelected = true
                                    submitForResult()
                                }
                                else -> {
                                    input_pin_checker_status.isSelected = false
                                    showPinInputWrongUI()
                                }
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

    private fun showPinInputRestoreUI() = GlobalScope.launch(Dispatchers.Main) {
        showPinInputUI()
        initRestoreUI()
    }

    private fun showPinVerifyUI() = GlobalScope.launch(Dispatchers.Main) {
        showPinInputUI()
        initVerifyUI(true).join()
    }

    private fun initRestoreUI(clearPin: Boolean = false) = GlobalScope.launch(Dispatchers.Main) {
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

    private fun initVerifyUI(clearPin: Boolean = false) = GlobalScope.launch(Dispatchers.Main) {
        initRestoreUI(clearPin).join()
        if (mRequestCode == KEY_VERIFY_PIN_FOR_SEND) {
            pin_toolbar.screen_title.setText(R.string.mozo_transfer_title)
            sub_title_pin.setText(R.string.mozo_pin_sub_title_send)
        } else {
            pin_toolbar.screen_title.setText(R.string.mozo_pin_title_verify)
            sub_title_pin.setText(R.string.mozo_pin_sub_title)
        }
    }

    private fun showPinInputConfirmUI() = GlobalScope.launch(Dispatchers.Main) {
        sub_title_pin.setText(R.string.mozo_pin_confirm_sub_title)
        text_content_pin.setText(R.string.mozo_pin_confirm_content)

        input_pin.setText("")
        text_content_pin.visible()
        input_pin_checker_status.visible()
        input_pin_checker_status.isSelected = false
    }

    private fun showPinCreatedUI() = GlobalScope.launch(Dispatchers.Main) {
        text_correct_pin.setText(R.string.mozo_pin_msg_create_success)
        text_correct_pin.visible()
        input_pin_checker_status.isSelected = true
        input_pin.visible()
        input_pin.isEnabled = false
        text_content_pin.visible()
        hideLoadingUI()
    }

    private fun showPinInputCorrectUI() = GlobalScope.launch(Dispatchers.Main) {
        showPinCreatedUI().join()
        text_correct_pin.setText(R.string.mozo_pin_msg_enter_correct)
    }

    private fun showPinInputWrongUI() = GlobalScope.launch(Dispatchers.Main) {
        text_incorrect_pin.visible()
    }

    private fun hidePinInputWrongUI() = GlobalScope.launch(Dispatchers.Main) {
        input_pin_checker_status.isSelected = false
        if (text_incorrect_pin.visibility != View.GONE)
            text_incorrect_pin.gone()
    }

    private fun showLoadingUI() = GlobalScope.launch(Dispatchers.Main) {
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

    private fun showErrorAndRetryUI() = GlobalScope.launch(Dispatchers.Main) {
        hideLoadingUI()
        error_container.visible()
        button_retry.click {
            submitForResult()
        }
    }

    private fun submitForResult() {
        GlobalScope.launch {
            delay(500)
            showLoadingUI().join()

            if (mRequestCode == KEY_CREATE_PIN) {
                MozoWallet.getInstance().executeSaveWallet(this@SecurityActivity, mPIN) {
                    if (!it) {
                        showErrorAndRetryUI()
                        return@executeSaveWallet
                    }
                    showPinCreatedUI()
                    finishResult()
                }
                return@launch
            }

            mPIN = input_pin.text.toString()
            val isCorrect = MozoWallet.getInstance().validatePinAsync(mPIN).await()

            when (mRequestCode) {
                KEY_ENTER_PIN -> {
                    showLoadingUI().join()
                    MozoWallet.getInstance().syncOnChainWallet(this@SecurityActivity, mPIN) {
                        initRestoreUI(!isCorrect).invokeOnCompletion {
                            if (isCorrect) {
                                showPinInputCorrectUI()
                                finishResult()
                            } else {
                                showPinInputWrongUI()
                            }
                        }
                    }
                }
                KEY_VERIFY_PIN,
                KEY_VERIFY_PIN_FOR_SEND -> {
                    initVerifyUI(!isCorrect).join()
                    if (isCorrect) {
                        showPinInputCorrectUI().join()
                        finishResult()
                    } else {
                        showPinInputWrongUI().join()
                    }
                }
            }
        }
    }

    private fun finishResult() = GlobalScope.launch {
        val start = System.currentTimeMillis()
        EventBus.getDefault().post(MessageEvent.Pin(mPIN, mRequestCode))

        if (System.currentTimeMillis() - start < mShowMessageDuration / 2) {
            delay(mShowMessageDuration)
        }
        withContext(Dispatchers.Main) {
            setResult(RESULT_OK, Intent().putExtra(KEY_DATA, mPIN))
            willReturnsResult = true
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

        fun start(context: Context, mode: Int) {
            Intent(context, SecurityActivity::class.java).apply {
                putExtra(KEY_MODE, mode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }

        fun startVerify(context: Context) {
            start(context, KEY_VERIFY_PIN)
        }
    }
}