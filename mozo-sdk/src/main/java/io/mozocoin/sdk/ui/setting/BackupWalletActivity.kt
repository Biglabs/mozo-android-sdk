package io.mozocoin.sdk.ui.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
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
import kotlinx.android.synthetic.main.activity_seed_word_verification.*
import kotlinx.android.synthetic.main.view_wallet_security_backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

internal class BackupWalletActivity : BaseActivity() {

    private val requestSecurityPin = 0x10

    private val allWords = mutableListOf<String>()
    private val words = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val walletInfo = MozoWallet.getInstance().getWallet(false)?.buildWalletInfo()
        if (walletInfo?.pin.isNullOrEmpty()) {
            EventBus.getDefault().register(this)
            SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN, requestSecurityPin)

        } else onReceivedPin(MessageEvent.Pin(
            walletInfo?.pin ?: "",
            SecurityActivity.KEY_VERIFY_PIN)
        )
    }

    private fun displaySeedWords() = GlobalScope.launch(Dispatchers.Main) {
        setContentView(R.layout.view_wallet_security_backup)

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

    @Subscribe
    fun onReceivedPin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this)

        val w = MozoWallet.getInstance().getWallet(false)
            ?.decrypt(event.pin)
            ?.mnemonicPhrases()
            ?.toMutableList()
        words.addAll(w ?: return)
        displaySeedWords()
    }

    private fun startVerifySeedWords() {
        setContentView(R.layout.activity_seed_word_verification)

        allWords.addAll(MnemonicUtils.getWords() ?: listOf())

        initWidget()
    }

    @SuppressLint("SetTextI18n")
    private fun initWidget() {
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

            edit.setOnFocusChangeListener { _, hasFocus ->
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
            tb_verify.showCloseButton(false)
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

    private fun validWords() =
        edit_verify_seed_1.text.toString() == words.getOrNull(txt_index_1.text.toString().toInt() - 1)
                && edit_verify_seed_2.text.toString() == words.getOrNull(txt_index_2.text.toString().toInt() - 1)
                && edit_verify_seed_3.text.toString() == words.getOrNull(txt_index_3.text.toString().toInt() - 1)
                && edit_verify_seed_4.text.toString() == words.getOrNull(txt_index_4.text.toString().toInt() - 1)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK && requestCode == requestSecurityPin) {
            finish()
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

}
