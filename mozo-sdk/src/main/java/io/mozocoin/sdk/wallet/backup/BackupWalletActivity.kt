package io.mozocoin.sdk.wallet.backup

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.databinding.ViewWalletConfirmPhrasesBinding
import io.mozocoin.sdk.databinding.ViewWalletDisplayPhrasesBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.hideKeyboard
import io.mozocoin.sdk.utils.showKeyboard
import io.mozocoin.sdk.utils.visible
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

internal class BackupWalletActivity : BaseActivity() {

    private lateinit var bindingConfirm: ViewWalletConfirmPhrasesBinding
    private lateinit var bindingDisplay: ViewWalletDisplayPhrasesBinding

    private val allWords = mutableListOf<String>()
    private var randomIndex: MutableList<Int>? = null
    private var randomWord = arrayOf("", "", "", "")

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SECURITY_PIN -> {
                if (resultCode != RESULT_OK) {
                    finish()
                    return
                }
                GlobalScope.launch {
                    val pin = data?.getStringExtra(SecurityActivity.KEY_DATA)
                    if (pin.isNullOrEmpty()) {
                        finish()
                        return@launch
                    }

                    MozoWallet.getInstance().getWallet()?.decrypt(pin)
                    displaySeedWords()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loadWallet() {
        MozoAuth.getInstance().syncProfile(this) {
            if (!it) return@syncProfile

            val wallet = MozoWallet.getInstance().getWallet(false)
            if (wallet == null) {
                // TODO OMG current wallet is null ?
                finish()
                return@syncProfile
            }

            if (wallet.isUnlocked()) {
                displaySeedWords()

            } else {
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN_FOR_BACKUP, REQUEST_SECURITY_PIN)
            }
        }
    }

    private fun displaySeedWords() = MainScope().launch {
        val words = MozoWallet.getInstance().getWallet()?.let {
            val phrase = it.mnemonicPhrases()?.toMutableList()
            it.lock()
            return@let phrase
        }
        if (words.isNullOrEmpty()) {
            finish()
            return@launch
        }

        randomIndex = randomItems()
        randomWord[0] = words[randomIndex!![0]]
        randomWord[1] = words[randomIndex!![1]]
        randomWord[2] = words[randomIndex!![2]]
        randomWord[3] = words[randomIndex!![3]]

        bindingDisplay = ViewWalletDisplayPhrasesBinding.inflate(layoutInflater)
        setContentView(bindingDisplay.root)
        bindingDisplay.toolbarMozoDisplayPhrases.apply {
            showBackButton(true)
            onBackPress = {
                EventBus.getDefault().post(MessageEvent.CloseActivities())
            }
            showCloseButton(false)
        }

        bindingDisplay.seedView.adapter = SeedWordAdapter(words)
        bindingDisplay.txtWarning.text = SpannableString("  " + getString(R.string.mozo_backup_warning)).apply {
            setSpan(ImageSpan(this@BackupWalletActivity, R.drawable.ic_warning),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        bindingDisplay.buttonStoredConfirm.click {
            it.isSelected = !it.isSelected
            bindingDisplay.buttonContinue.isEnabled = it.isSelected
        }

        bindingDisplay.buttonContinue.click {
            startVerifySeedWords()
        }
    }

    private fun startVerifySeedWords() {
        bindingConfirm = ViewWalletConfirmPhrasesBinding.inflate(layoutInflater)
        setContentView(bindingConfirm.root)

        allWords.addAll(MnemonicUtils.getWords() ?: listOf())

        randomIndex ?: return
        bindingConfirm.txtIndex1.text = "${randomIndex!![0] + 1}"
        bindingConfirm.txtIndex2.text = "${randomIndex!![1] + 1}"
        bindingConfirm.txtIndex3.text = "${randomIndex!![2] + 1}"
        bindingConfirm.txtIndex4.text = "${randomIndex!![3] + 1}"

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
                    val exist = allWords.contains(edit.text.toString())
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
            if (bindingConfirm.loSuccess.isVisible)
                return@click finish()

            if (!validWords())
                return@click MessageDialog.show(this,
                        getString(R.string.mozo_backup_confirm_failed))

            bindingConfirm.buttonFinish.text = getString(R.string.mozo_button_got_it)
            bindingConfirm.toolbarMozo.showCloseButton(false)
            it.hideKeyboard()
            //TODO call API
            bindingConfirm.loSuccess.visible()
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
        return bindingConfirm.editVerifySeed1.text.toString() == randomWord[0]
                && bindingConfirm.editVerifySeed2.text.toString() == randomWord[1]
                && bindingConfirm.editVerifySeed3.text.toString() == randomWord[2]
                && bindingConfirm.editVerifySeed4.text.toString() == randomWord[3]
    }

    companion object {
        private const val REQUEST_SECURITY_PIN = 0x10
    }
}
