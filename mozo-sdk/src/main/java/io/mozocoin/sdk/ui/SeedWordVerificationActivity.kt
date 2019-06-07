package io.mozocoin.sdk.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isGone
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.onTextChanged
import io.mozocoin.sdk.utils.visible
import kotlinx.android.synthetic.main.activity_seed_word_verification.*
import org.web3j.crypto.MnemonicUtils
import kotlin.random.Random

class SeedWordVerificationActivity : AppCompatActivity() {

    private val words = mutableListOf<String>()
    private val allWords = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seed_word_verification)

        words.addAll(MozoWallet.getInstance().getWallet(true)?.mnemonicPhrases() ?: listOf())
        allWords.addAll(MnemonicUtils.getWords() ?: listOf())

        initWidget()

        tb_verify.onClosePress = ::finish

        button_finish.click {
            if (lo_success.isGone) {
                lo_success.visible()
                button_finish.text = getString(R.string.mozo_text_backup_wallet_gotit)
                tb_verify.showCloseButton(false)
            } else finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initWidget() {
        val randoms = randomItems()
        txt_index_1.text = "${randoms[0] + 1}"
        txt_index_2.text = "${randoms[1] + 1}"
        txt_index_3.text = "${randoms[2] + 1}"
        txt_index_4.text = "${randoms[3] + 1}"

        listOf(edit_verify_seed_1,
            edit_verify_seed_2,
            edit_verify_seed_3,
            edit_verify_seed_4).forEach { edit ->
            edit.onTextChanged {
                validateWords(edit)
            }
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
            sort()
            return this
        }
    }

    private fun validateWords(edit: AppCompatEditText) {
        val exist = allWords.contains(edit.text.toString())
        edit.isActivated = exist
        edit.isSelected = !exist

        button_finish.isEnabled =
            edit_verify_seed_1.text.toString() == words.getOrNull(txt_index_1.text.toString().toInt() - 1)
                    && edit_verify_seed_2.text.toString() == words.getOrNull(txt_index_2.text.toString().toInt() - 1)
                    && edit_verify_seed_3.text.toString() == words.getOrNull(txt_index_3.text.toString().toInt() - 1)
                    && edit_verify_seed_4.text.toString() == words.getOrNull(txt_index_4.text.toString().toInt() - 1)

    }

}
