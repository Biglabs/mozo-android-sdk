package io.mozocoin.sdk.wallet.backup

import android.annotation.SuppressLint
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
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.hideKeyboard
import io.mozocoin.sdk.utils.showKeyboard
import io.mozocoin.sdk.utils.visible
import kotlinx.android.synthetic.main.view_wallet_confirm_phrases.*
import kotlinx.android.synthetic.main.view_wallet_display_phrases.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

internal class BackupWalletActivity : BaseActivity() {

    private val requestSecurityPin = 0x10

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
            requestSecurityPin -> {
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
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN, requestSecurityPin)
            }
        }
    }

    private fun displaySeedWords() = GlobalScope.launch(Dispatchers.Main) {
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

        setContentView(R.layout.view_wallet_display_phrases)
        toolbar_mozo_display_phrases?.apply {
            showBackButton(true)
            onBackPress = {
                EventBus.getDefault().post(MessageEvent.CloseActivities())
            }
            showCloseButton(false)
        }

        seed_view.adapter = SeedWordAdapter(words)
        txt_warning.text = SpannableString("  " + getString(R.string.mozo_backup_warning)).apply {
            setSpan(ImageSpan(this@BackupWalletActivity, R.drawable.ic_warning),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        button_stored_confirm.click {
            it.isSelected = !it.isSelected
            button_continue.isEnabled = it.isSelected
        }

        button_continue.click {
            startVerifySeedWords()
        }
    }

    private fun startVerifySeedWords() {
        setContentView(R.layout.view_wallet_confirm_phrases)

        allWords.addAll(MnemonicUtils.getWords() ?: listOf())

        initWidget()
    }

    @SuppressLint("SetTextI18n")
    private fun initWidget() {
        randomIndex ?: return
        txt_index_1.text = "${randomIndex!![0] + 1}"
        txt_index_2.text = "${randomIndex!![1] + 1}"
        txt_index_3.text = "${randomIndex!![2] + 1}"
        txt_index_4.text = "${randomIndex!![3] + 1}"

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
                    val exist = allWords.contains(edit.text.toString())
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
            if (lo_success.isVisible)
                return@click finish()

            if (!validWords())
                return@click MessageDialog.show(this,
                        getString(R.string.mozo_backup_wallet_invalid_recovery_phrase))

            button_finish.text = getString(R.string.mozo_text_backup_wallet_gotit)
            toolbar_mozo.showCloseButton(false)
            //TODO call API
            lo_success.visible()
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
        return edit_verify_seed_1.text.toString() == randomWord[0]
                && edit_verify_seed_2.text.toString() == randomWord[1]
                && edit_verify_seed_3.text.toString() == randomWord[2]
                && edit_verify_seed_4.text.toString() == randomWord[3]
    }
}
