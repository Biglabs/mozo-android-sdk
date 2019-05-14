package io.mozocoin.sdk.wallet.reset

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.WalletHelper
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.hideKeyboard
import io.mozocoin.sdk.utils.visible
import kotlinx.android.synthetic.main.fragment_reset_enter_seed.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.MnemonicUtils

internal class EnterSeedFragment : ResetPinBaseFragment() {

    private lateinit var clipboard: ClipboardManager

    private var mInteractionListener: InteractionListener? = null
    private var mInputView: Array<EditText>? = null
    private var isCancelableRightButton = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboard = (activity?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) ?: return)
                as ClipboardManager

        if (context is InteractionListener) {
            mInteractionListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_reset_enter_seed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mInputView = arrayOf(
                reset_pin_seed_1,
                reset_pin_seed_2,
                reset_pin_seed_3,
                reset_pin_seed_4,
                reset_pin_seed_5,
                reset_pin_seed_6,
                reset_pin_seed_7,
                reset_pin_seed_8,
                reset_pin_seed_9,
                reset_pin_seed_10,
                reset_pin_seed_11,
                reset_pin_seed_12
        )

        mInputView?.forEach {
            it.onFocusChangeListener = onFocusChange
        }
        reset_pin_sub_title?.onFocusChangeListener = onFocusChange

        reset_pin_seed_12?.setOnEditorActionListener { _, actionId, _ ->
            if (KeyEvent.KEYCODE_ENDCALL == actionId) {
                reset_pin_sub_title?.requestFocus()
                reset_pin_sub_title?.hideKeyboard()
            }
            false
        }
    }

    override fun onStart() {
        super.onStart()
        reset_pin_seed_1?.requestFocus()
    }

    private val onFocusChange = View.OnFocusChangeListener { v, hasFocus ->
        if (v.id == R.id.reset_pin_sub_title) {
            checkAllInputs()
            return@OnFocusChangeListener
        }

        if (!hasFocus && v is EditText) {
            checkSingleInput(v)
        } else {
            v.isActivated = false
            v.isSelected = false
            /**
             * Advanced: auto detect Clipboard to fill in seed phrase
             * if (v.id == R.id.reset_pin_seed_1) detectClipboard()
             */

            if (v.id == R.id.reset_pin_seed_1) detectClipboard()
        }
    }


    private fun checkSingleInput(v: EditText) {
        val seed = v.text.toString()
        GlobalScope.launch {
            val word = MnemonicUtils.getWords().firstOrNull { it.equals(seed, true) }
            withContext(Dispatchers.Main) {
                v.isActivated = !word.isNullOrEmpty()
                v.isSelected = word.isNullOrEmpty()
            }
        }
    }

    private fun checkAllInputs() {
        var hasIncorrectWord = false

        mInputView?.forEachIndexed { index, v ->
            val seed = v.text.toString()
            GlobalScope.launch {
                val word = MnemonicUtils.getWords().firstOrNull { it.equals(seed, true) }
                if (word.isNullOrEmpty()) hasIncorrectWord = true
                withContext(Dispatchers.Main) {
                    v.isActivated = !word.isNullOrEmpty()
                    v.isSelected = word.isNullOrEmpty()
                    if (index == (mInputView?.size ?: 0) - 1) {
                        showInlineError(R.string.mozo_pin_reset_msg_error, hasIncorrectWord)
                        mInteractionListener?.getCloseButton()?.isEnabled = !hasIncorrectWord
                    }
                }
            }
        }
    }

    private fun finalVerify() {
        reset_pin_loading_view?.visible()
        mInteractionListener?.getCloseButton()?.setText(R.string.mozo_button_cancel)
        isCancelableRightButton = true

        val mnemonic = mInputView?.map { it.text.toString().trim() }?.joinToString(separator = " ") { s -> s }
        if (!mnemonic.isNullOrEmpty() && MnemonicUtils.validateMnemonic(mnemonic)) {
            GlobalScope.launch {
                val wallet = WalletHelper(mnemonic)
                val isCorrectWallet = wallet.buildWalletInfo().offchainAddress
                        .equals(MozoWallet.getInstance().getAddress(), ignoreCase = true)

                if (isCorrectWallet) {
                    mInteractionListener?.getResetPinModel()?.setData(wallet)
                    mInteractionListener?.requestEnterPin()
                } else {
                    showInlineError(R.string.mozo_pin_reset_msg_error_incorrect, false)
                }
            }

        } else {
            showInlineError(R.string.mozo_pin_reset_msg_error_incorrect, true)
        }
    }

    private fun showInlineError(@StringRes error: Int, showing: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        reset_pin_sub_title?.apply {
            setText(
                    if (showing) error
                    else R.string.mozo_pin_reset_sub_title
            )
            isSelected = showing
        }

        reset_pin_loading_view?.gone()
        mInteractionListener?.getCloseButton()?.setText(R.string.mozo_button_submit)
        isCancelableRightButton = false
    }

    override fun onCloseClicked() {
        if (!isVisible) return
        if (isCancelableRightButton) {
            activity?.finish()

        } else {
            reset_pin_sub_title?.requestFocus()
            reset_pin_sub_title?.hideKeyboard()

            Handler().postDelayed({ finalVerify() }, 200)
        }
    }

    private fun detectClipboard() {
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()?.split(" ")
        if (clipText?.size ?: 0 < 2) return
        clipText?.forEachIndexed { index, s ->
            mInputView?.getOrNull(index)?.setText(s)
        }
        checkAllInputs()
    }

    companion object {
        fun newInstance() = EnterSeedFragment()
    }
}