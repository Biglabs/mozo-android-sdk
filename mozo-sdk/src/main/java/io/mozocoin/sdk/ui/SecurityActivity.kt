package io.mozocoin.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.postDelayed
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.databinding.ViewWalletAutoPinNoticeBinding
import io.mozocoin.sdk.databinding.ViewWalletConfirmPhrasesBinding
import io.mozocoin.sdk.databinding.ViewWalletDisplayPhrasesBinding
import io.mozocoin.sdk.databinding.ViewWalletSecurityBinding
import io.mozocoin.sdk.transaction.TransactionFormActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import io.mozocoin.sdk.wallet.backup.SeedWordAdapter
import io.mozocoin.sdk.wallet.reset.ResetPinActivity
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

internal class SecurityActivity : BaseActivity() {

    private lateinit var bindingPin: ViewWalletAutoPinNoticeBinding
    private lateinit var bindingConfirm: ViewWalletConfirmPhrasesBinding
    private lateinit var bindingDisplay: ViewWalletDisplayPhrasesBinding
    private lateinit var bindingSecurity: ViewWalletSecurityBinding

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
            KEY_ENTER_PIN_FOR_SEND,
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
        if (this::bindingSecurity.isInitialized) {
            bindingSecurity.inputPin.hideKeyboard()
        }
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
        bindingDisplay = ViewWalletDisplayPhrasesBinding.inflate(layoutInflater)
        setContentView(bindingDisplay.root)
        bindingDisplay.toolbarMozoDisplayPhrases.showBackButton(true)

        val words = MozoWallet.getInstance().getWallet(true)?.mnemonicPhrases()?.toMutableList()
        bindingDisplay.seedView.adapter = SeedWordAdapter(words ?: mutableListOf())
        bindingDisplay.txtWarning.text = SpannableString("  " + getString(R.string.mozo_backup_warning)).apply {
            setSpan(ImageSpan(this@SecurityActivity, R.drawable.ic_warning),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        bindingDisplay.buttonStoredConfirm.click {
            it.isSelected = !it.isSelected
            bindingDisplay.buttonContinue.isEnabled = it.isSelected
        }

        bindingDisplay.buttonContinue.click {
            showRecoveryPhraseConfirmationUI()
        }
    }

    /**
     * START Recovery phrases confirmation
     */
    private fun showRecoveryPhraseConfirmationUI() {
        bindingConfirm = ViewWalletConfirmPhrasesBinding.inflate(layoutInflater)
        setContentView(bindingConfirm.root)

        val randoms = randomItems()
        bindingConfirm.txtIndex1.text = (randoms[0] + 1).toString()
        bindingConfirm.txtIndex2.text = (randoms[1] + 1).toString()
        bindingConfirm.txtIndex3.text = (randoms[2] + 1).toString()
        bindingConfirm.txtIndex4.text = (randoms[3] + 1).toString()

        val edits = listOf(
                bindingConfirm.editVerifySeed1,
                bindingConfirm.editVerifySeed2,
                bindingConfirm.editVerifySeed3,
                bindingConfirm.editVerifySeed4
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

        bindingConfirm.loContent.click {
            currentFocus?.clearFocus()
            currentFocus?.hideKeyboard()
        }

        currentFocus?.showKeyboard()

        bindingConfirm.buttonFinish.click {
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
        return !words.isNullOrEmpty()
                && bindingConfirm.editVerifySeed1.text.toString() == words.getOrNull(bindingConfirm.txtIndex1.text.toString().toInt() - 1)
                && bindingConfirm.editVerifySeed2.text.toString() == words.getOrNull(bindingConfirm.txtIndex2.text.toString().toInt() - 1)
                && bindingConfirm.editVerifySeed3.text.toString() == words.getOrNull(bindingConfirm.txtIndex3.text.toString().toInt() - 1)
                && bindingConfirm.editVerifySeed4.text.toString() == words.getOrNull(bindingConfirm.txtIndex4.text.toString().toInt() - 1)
    }

    /**
     * END Recovery phrases confirmation
     */

    private var isShowAutoPinNotice = true

    private fun showMsg4AutoPin() {
        isAllowBackPress = false

        bindingPin = ViewWalletAutoPinNoticeBinding.inflate(layoutInflater)
        setContentView(bindingPin.root)

        bindingPin.buttonDntShowAgain.click {
            it.isSelected = !it.isSelected
            isShowAutoPinNotice = !it.isSelected
        }

        bindingPin.root.postDelayed(Constant.AUTO_PIN_WAITING_TIMER) {
            SharedPrefsUtils.setShowAutoPinNotice(isShowAutoPinNotice)
            executeAutoPin()
        }
    }

    private fun executeAutoPin() {
        EventBus.getDefault().post(MessageEvent.Pin(null, mRequestCode))
        setResult(RESULT_OK, Intent().putExtra(KEY_DATA, mPIN))
        willReturnsResult = true
        finish()
    }

    private fun showPinInputUI() {
        bindingSecurity = ViewWalletSecurityBinding.inflate(layoutInflater)
        setContentView(bindingSecurity.root)

        bindingSecurity.pinToolbar.findViewById<TextView>(R.id.screen_title).setText(R.string.mozo_pin_title)
        bindingSecurity.subTitlePin.setText(R.string.mozo_pin_sub_title)
        bindingSecurity.textContentPin.setText(R.string.mozo_pin_content)

        bindingSecurity.inputPin.apply {
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
                                    bindingSecurity.inputPinCheckerStatus.apply {
                                        isSelected = true
                                        visible()
                                    }
                                    submitForResult()
                                }
                                else -> {
                                    bindingSecurity.inputPinCheckerStatus.isSelected = false
                                    showPinInputWrongUI()
                                }
                            }
                        }
                        else -> submitForResult()
                    }
                }
            }

            postDelayed(500) {
                showKeyboard()
            }
        }
        bindingSecurity.inputPinCheckerStatus.gone()
        bindingSecurity.pinForgotMsg.click {
            launchActivity<ResetPinActivity>(RC_REST_PIN) { }
        }
    }

    private fun showPinInputRestoreUI() = MainScope().launch {
        showPinInputUI()
        initRestoreUI()
    }

    private fun showPinVerifyUI() = MainScope().launch {
        showPinInputUI()
        initVerifyUI(true).join()
    }

    private fun initRestoreUI(clearPin: Boolean = false) = MainScope().launch {
        bindingSecurity.pinToolbar.findViewById<TextView>(R.id.screen_title).setText(R.string.mozo_pin_title_restore)
        bindingSecurity.subTitlePin.setText(R.string.mozo_pin_sub_title_restore)

        bindingSecurity.inputPin.apply {
            visible()
            if (clearPin) {
                text?.clear()
            }
        }
        bindingSecurity.textContentPin.visible()
        bindingSecurity.pinForgotGroup.visible()
        hideLoadingUI()
    }

    private fun initVerifyUI(clearPin: Boolean = false) = MainScope().launch {
        initRestoreUI(clearPin).join()
        val screenTitle = bindingSecurity.pinToolbar.findViewById<TextView>(R.id.screen_title)
        when (mRequestCode) {
            KEY_ENTER_PIN_FOR_SEND -> {
                screenTitle.setText(R.string.mozo_pin_sub_title)
                bindingSecurity.subTitlePin.setText(R.string.mozo_pin_sub_title_send)
            }
            KEY_VERIFY_PIN_FOR_SEND -> {
                screenTitle.setText(R.string.mozo_transfer_title)
                bindingSecurity.subTitlePin.setText(R.string.mozo_pin_sub_title_send)
            }
            KEY_VERIFY_PIN_FOR_BACKUP -> {
                screenTitle.setText(R.string.mozo_pin_sub_title)
                bindingSecurity.subTitlePin.setText(R.string.mozo_pin_change_sub_enter_current)
            }
            else -> {
                screenTitle.setText(R.string.mozo_pin_sub_title)
                bindingSecurity.subTitlePin.setText(R.string.mozo_pin_sub_title_common)
            }
        }
    }

    private fun showPinInputConfirmUI() = MainScope().launch {
        bindingSecurity.subTitlePin.setText(R.string.mozo_pin_confirm_sub_title)
        bindingSecurity.textContentPin.setText(R.string.mozo_pin_confirm_content)

        bindingSecurity.inputPin.setText("")
        bindingSecurity.textContentPin.visible()
        bindingSecurity.inputPinCheckerStatus.visible()
        bindingSecurity.inputPinCheckerStatus.isSelected = false
    }

    private fun showPinCreatedUI() = MainScope().launch {
        bindingSecurity.textCorrectPin.setText(R.string.mozo_pin_msg_create_success)
        bindingSecurity.textCorrectPin.visible()
        bindingSecurity.inputPinCheckerStatus.apply {
            isSelected = true
            visible()
        }
        bindingSecurity.inputPin.visible()
        bindingSecurity.inputPin.isEnabled = false
        bindingSecurity.textContentPin.visible()
        bindingSecurity.pinForgotGroup.gone()
        hideLoadingUI()
    }

    private fun showPinInputCorrectUI() = MainScope().launch {
        showPinCreatedUI().join()
        bindingSecurity.inputPinCheckerStatus.gone()
        bindingSecurity.textCorrectPin.setText(R.string.mozo_pin_msg_enter_correct)
        isAllowBackPress = false
    }

    private fun showPinInputWrongUI() = MainScope().launch {
        bindingSecurity.textIncorrectPin.visible()
        bindingSecurity.inputPin.showKeyboard()
    }

    private fun hidePinInputWrongUI() = MainScope().launch {
        bindingSecurity.inputPinCheckerStatus.isSelected = false
        gone(
                bindingSecurity.textCorrectPin,
                bindingSecurity.textIncorrectPin
        )
    }

    private fun showLoadingUI() = MainScope().launch {
        isAllowBackPress = false
        gone(
                bindingSecurity.textCorrectPin,
                bindingSecurity.textIncorrectPin,
                bindingSecurity.inputPin,
                bindingSecurity.inputPinCheckerStatus,
                bindingSecurity.textContentPin,
                bindingSecurity.errorContainer,
                bindingSecurity.pinForgotGroup
        )

        visible(bindingSecurity.inputLoadingIndicator)
    }

    private fun hideLoadingUI() {
        isAllowBackPress = true
        gone(bindingSecurity.inputLoadingIndicator)
    }

    private fun showErrorAndRetryUI() = MainScope().launch {
        hideLoadingUI()
        bindingSecurity.errorContainer.visible()
        bindingSecurity.buttonRetry.click {
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

            mPIN = bindingSecurity.inputPin.text.toString()
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
                KEY_ENTER_PIN_FOR_SEND,
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
        const val KEY_ENTER_PIN_FOR_SEND = 0x003
        const val KEY_VERIFY_PIN = 0x004
        const val KEY_VERIFY_PIN_FOR_SEND = 0x005
        const val KEY_VERIFY_PIN_FOR_BACKUP = 0x006

        const val KEY_DATA = "KEY_DATA"

        fun isNeedCallbackForSign(requestCode: Int) =
                requestCode == KEY_ENTER_PIN_FOR_SEND
                        || requestCode == KEY_VERIFY_PIN
                        || requestCode == KEY_VERIFY_PIN_FOR_SEND

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

        fun startVerify(context: Context, enter4Send: Boolean = false) {
            start(
                    context,
                    when {
                        context is TransactionFormActivity -> KEY_VERIFY_PIN_FOR_SEND
                        enter4Send -> KEY_ENTER_PIN_FOR_SEND
                        else -> KEY_VERIFY_PIN
                    }
            )
        }
    }
}