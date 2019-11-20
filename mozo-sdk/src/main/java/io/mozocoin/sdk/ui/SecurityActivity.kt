package io.mozocoin.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.transaction.TransactionFormActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import io.mozocoin.sdk.wallet.backup.SeedWordAdapter
import io.mozocoin.sdk.wallet.reset.ResetPinActivity
import kotlinx.android.synthetic.main.view_toolbar.view.*
import kotlinx.android.synthetic.main.view_wallet_auto_pin_notice.*
import kotlinx.android.synthetic.main.view_wallet_confirm_phrases.*
import kotlinx.android.synthetic.main.view_wallet_display_phrases.*
import kotlinx.android.synthetic.main.view_wallet_security.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

internal class SecurityActivity : BaseActivity() {

    private var mPIN = ""
    private var mPINLength = 0
    private var mShowMessageDuration = 0L
    private var mRequestCode = -1
    private var willReturnsResult = false
    private var mFinishJob: Job? = null

    private var isAllowBackPress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPINLength = getInteger(R.integer.security_pin_length)
        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration).toLong()

        mRequestCode = intent.getIntExtra(KEY_MODE, mRequestCode)

        mFinishJob?.cancel()
        mFinishJob = null

        when (mRequestCode) {
            KEY_CREATE_PIN -> showRecoveryPhraseUI()
            KEY_ENTER_PIN -> showPinInputRestoreUI()
            KEY_VERIFY_PIN,
            KEY_VERIFY_PIN_FOR_SEND,
            KEY_VERIFY_PIN_FOR_BACKUP -> {
                if (MozoWallet.getInstance().getWallet()?.isUnlocked() == true) {
                    if (SharedPrefsUtils.getShowAutoPinNotice()) showMsg4AutoPin()
                    else executeAutoPin()

                } else showPinVerifyUI()
            }
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
        mPIN = ""
        mFinishJob?.cancel()
        mFinishJob = null
        if (!willReturnsResult) {
            EventBus.getDefault().post(MessageEvent.UserCancel())
        }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == RC_REST_PIN) {
            hidePinInputWrongUI()
        }
    }

    override fun onBackPressed() {
        if (isAllowBackPress) super.onBackPressed()
    }

    private fun showRecoveryPhraseUI() {
        setContentView(R.layout.view_wallet_display_phrases)
        toolbar_mozo_display_phrases?.showBackButton(true)

        val words = MozoWallet.getInstance().getWallet(true)?.mnemonicPhrases()?.toMutableList()
        seed_view.adapter = SeedWordAdapter(words ?: mutableListOf())
        txt_warning.text = SpannableString("  " + getString(R.string.mozo_backup_warning)).apply {
            setSpan(ImageSpan(this@SecurityActivity, R.drawable.ic_warning),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        button_stored_confirm.click {
            it.isSelected = !it.isSelected
            button_continue.isEnabled = it.isSelected
        }

        button_continue.click {
            showRecoveryPhraseConfirmationUI()
        }
    }

    /**
     * START Recovery phrases confirmation
     */
    private fun showRecoveryPhraseConfirmationUI() {
        setContentView(R.layout.view_wallet_confirm_phrases)

        val randoms = randomItems()
        txt_index_1.text = "${randoms[0] + 1}"
        txt_index_2.text = "${randoms[1] + 1}"
        txt_index_3.text = "${randoms[2] + 1}"
        txt_index_4.text = "${randoms[3] + 1}"

        val edits = listOf(
                edit_verify_seed_1,
                edit_verify_seed_2,
                edit_verify_seed_3,
                edit_verify_seed_4
        )

        edits.forEach { edit ->
            edit.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    edit.clearFocus()
                    edit.hideKeyboard()
                }

                false
            }

            edit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val exist = MnemonicUtils.getWords().contains(edit.text.toString())
                    edit.isActivated = exist
                    edit.isSelected = !exist
                } else {
                    edit.isActivated = false
                    edit.isSelected = false
                }
            }
        }

        lo_content.click {
            currentFocus?.clearFocus()
            currentFocus?.hideKeyboard()
        }

        currentFocus?.showKeyboard()

        button_finish.click {
            if (!validWords())
                return@click MessageDialog.show(this,
                        getString(R.string.mozo_backup_confirm_failed))

            showPinInputUI()
        }
    }

    private fun randomItems(): MutableList<Int> {
        fun random(from: Int, to: Int, except: Int): Int {
            val i = Random.nextInt(from, to)
            return if (i == except)
                random(from, to, except)
            else i
        }

        mutableListOf(Random.nextInt(0, 6)).apply {
            add(random(0, 6, first()))
            add(Random.nextInt(6, 12))
            add(random(6, 12, this[2]))
            return this
        }
    }

    private fun validWords(): Boolean {
        val words = MozoWallet.getInstance().getWallet(false)?.mnemonicPhrases()?.toMutableList()
        return !words.isNullOrEmpty() && edit_verify_seed_1.text.toString() == words.getOrNull(txt_index_1.text.toString().toInt() - 1)
                && edit_verify_seed_2.text.toString() == words.getOrNull(txt_index_2.text.toString().toInt() - 1)
                && edit_verify_seed_3.text.toString() == words.getOrNull(txt_index_3.text.toString().toInt() - 1)
                && edit_verify_seed_4.text.toString() == words.getOrNull(txt_index_4.text.toString().toInt() - 1)
    }

    /**
     * END Recovery phrases confirmation
     */

    private var isShowAutoPinNotice = true

    private fun showMsg4AutoPin() {
        isAllowBackPress = false
        setContentView(R.layout.view_wallet_auto_pin_notice)

        button_dnt_show_again?.click {
            it.isSelected = !it.isSelected
            isShowAutoPinNotice = !it.isSelected
        }

        Handler().postDelayed({
            SharedPrefsUtils.setShowAutoPinNotice(isShowAutoPinNotice)
            executeAutoPin()

        }, 10000)
    }

    private fun executeAutoPin() {
        EventBus.getDefault().post(MessageEvent.Pin(null, mRequestCode))
        setResult(RESULT_OK, Intent().putExtra(KEY_DATA, mPIN))
        willReturnsResult = true
        finish()
    }

    private fun showPinInputUI() {
        setContentView(R.layout.view_wallet_security)

        pin_toolbar.screen_title.setText(R.string.mozo_pin_title)
        sub_title_pin.setText(R.string.mozo_pin_sub_title)
        text_content_pin.setText(R.string.mozo_pin_content)

        input_pin?.apply {
            onBackPress { finishAndRemoveTask() }
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
        input_pin_checker_status?.gone()
        pin_forgot_msg?.click {
            startActivityForResult(Intent(this, ResetPinActivity::class.java), RC_REST_PIN)
        }
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
        pin_forgot_group.visible()
        hideLoadingUI()
    }

    private fun initVerifyUI(clearPin: Boolean = false) = GlobalScope.launch(Dispatchers.Main) {
        initRestoreUI(clearPin).join()
        when (mRequestCode) {
            KEY_VERIFY_PIN_FOR_SEND -> {
                pin_toolbar.screen_title.setText(R.string.mozo_transfer_title)
                sub_title_pin.setText(R.string.mozo_pin_sub_title_send)
            }
            KEY_VERIFY_PIN_FOR_BACKUP -> {
                pin_toolbar.screen_title.setText(R.string.mozo_pin_sub_title)
                sub_title_pin.setText(R.string.mozo_pin_change_sub_enter_current)
            }
            else -> {
                pin_toolbar.screen_title.setText(R.string.mozo_pin_sub_title)
                sub_title_pin.setText(R.string.mozo_pin_sub_title_commom)
            }
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
        pin_forgot_group.gone()
        hideLoadingUI()
    }

    private fun showPinInputCorrectUI() = GlobalScope.launch(Dispatchers.Main) {
        showPinCreatedUI().join()
        text_correct_pin.setText(R.string.mozo_pin_msg_enter_correct)
        isAllowBackPress = false
    }

    private fun showPinInputWrongUI() = GlobalScope.launch(Dispatchers.Main) {
        text_incorrect_pin.visible()
        input_pin?.showKeyboard()
    }

    private fun hidePinInputWrongUI() = GlobalScope.launch(Dispatchers.Main) {
        input_pin_checker_status.isSelected = false
        if (text_incorrect_pin.visibility != View.GONE) text_incorrect_pin.gone()
    }

    private fun showLoadingUI() = GlobalScope.launch(Dispatchers.Main) {
        isAllowBackPress = false
        gone(text_correct_pin,
                text_incorrect_pin,
                input_pin,
                input_pin_checker_status,
                text_content_pin,
                error_container,
                pin_forgot_group)

        visible(input_loading_indicator)
    }

    private fun hideLoadingUI() {
        isAllowBackPress = true
        gone(input_loading_indicator)
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
                    mFinishJob = finishResult {
                        showPinCreatedUI()
                    }
                }
                return@launch
            }

            mPIN = input_pin.text.toString()
            val isCorrect = MozoWallet.getInstance().validatePinAsync(mPIN).await()

            when (mRequestCode) {
                KEY_ENTER_PIN -> {
                    showLoadingUI().join()
                    MozoWallet.getInstance().syncOnChainWallet(this@SecurityActivity, mPIN) {
                        if (isCorrect) {
                            mFinishJob = finishResult {
                                initRestoreUI(clearPin = false).invokeOnCompletion { showPinInputCorrectUI() }
                            }
                        } else {
                            initRestoreUI(clearPin = true).invokeOnCompletion { showPinInputWrongUI() }
                        }
                    }
                }
                KEY_VERIFY_PIN,
                KEY_VERIFY_PIN_FOR_SEND,
                KEY_VERIFY_PIN_FOR_BACKUP -> {
                    initVerifyUI(!isCorrect).join()
                    if (isCorrect) {
                        mFinishJob = finishResult {
                            showPinInputCorrectUI()
                        }
                    } else {
                        showPinInputWrongUI().join()
                    }
                }
            }
        }
    }

    private fun finishResult(callback: () -> Unit) = GlobalScope.launch {
        EventBus.getDefault().post(MessageEvent.Pin(mPIN, mRequestCode))
        withContext(Dispatchers.Main) {
            callback.invoke()
            delay(mShowMessageDuration)
            setResult(RESULT_OK, Intent().putExtra(KEY_DATA, mPIN))
            willReturnsResult = true
            finish()
        }
    }

    companion object {
        private const val KEY_MODE = "KEY_MODE"
        private const val RC_REST_PIN = 555

        const val KEY_CREATE_PIN = 0x001
        const val KEY_ENTER_PIN = 0x002
        const val KEY_VERIFY_PIN = 0x003
        const val KEY_VERIFY_PIN_FOR_SEND = 0x004
        const val KEY_VERIFY_PIN_FOR_BACKUP = 0x005

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
            start(
                    context,
                    if (context is TransactionFormActivity) KEY_VERIFY_PIN_FOR_SEND
                    else KEY_VERIFY_PIN
            )
        }
    }
}