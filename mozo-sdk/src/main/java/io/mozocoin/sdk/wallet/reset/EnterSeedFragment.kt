package io.mozocoin.sdk.wallet.reset

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.WalletHelper
import io.mozocoin.sdk.databinding.FragmentResetEnterSeedBinding
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.hideKeyboard
import io.mozocoin.sdk.utils.visible
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.web3j.crypto.MnemonicUtils

internal class EnterSeedFragment : ResetPinBaseFragment() {

    private var _binding: FragmentResetEnterSeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var clipboard: ClipboardManager

    private var mInteractionListener: InteractionListener? = null
    private var mInputView: Array<EditText>? = null
    private var isCancelableRightButton = false

    private var verifyJob: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboard = (activity?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) ?: return)
                as ClipboardManager

        if (context is InteractionListener) {
            mInteractionListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResetEnterSeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mInputView = arrayOf(
                binding.resetPinSeed1,
                binding.resetPinSeed2,
                binding.resetPinSeed3,
                binding.resetPinSeed4,
                binding.resetPinSeed5,
                binding.resetPinSeed6,
                binding.resetPinSeed7,
                binding.resetPinSeed8,
                binding.resetPinSeed9,
                binding.resetPinSeed10,
                binding.resetPinSeed11,
                binding.resetPinSeed12
        )

        mInputView?.forEach {
            it.onFocusChangeListener = onFocusChange
        }
        binding.resetPinSubTitle.onFocusChangeListener = onFocusChange

        binding.resetPinSeed12.setOnEditorActionListener { _, actionId, _ ->
            if (KeyEvent.KEYCODE_ENDCALL == actionId) {
                binding.resetPinSubTitle.requestFocus()
                binding.resetPinSubTitle.hideKeyboard()

                checkAllInputs()
            }
            false
        }
        /**
         * Advanced: auto detect Clipboard to fill in seed phrase
         */
        detectClipboard()
    }

    override fun onStart() {
        super.onStart()
        binding.resetPinSeed1.requestFocus()
    }

    override fun onDestroyView() {
        verifyJob?.cancel()
        verifyJob = null
        super.onDestroyView()
        _binding = null
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
        }
    }

    private fun checkSingleInput(v: EditText) {
        val seed = v.text.toString()
        verifyJob = MozoSDK.scope.launch {
            val word = MnemonicUtils.getWords().firstOrNull { it.equals(seed, true) }
            withContext(Dispatchers.Main) {
                v.isActivated = !word.isNullOrEmpty()
                v.isSelected = word.isNullOrEmpty()
                showInlineError(R.string.mozo_pin_reset_msg_error, word.isNullOrEmpty()).join()
            }
        }
    }

    private fun checkAllInputs() {
        var hasIncorrectWord = false

        mInputView?.forEachIndexed { index, v ->
            val seed = v.text.toString()
            verifyJob = MozoSDK.scope.launch {
                val word = MnemonicUtils.getWords().firstOrNull { it.equals(seed, true) }
                if (word.isNullOrEmpty()) hasIncorrectWord = true
                withContext(Dispatchers.Main) {
                    v.isActivated = !word.isNullOrEmpty()
                    v.isSelected = word.isNullOrEmpty()
                    if (index == (mInputView?.size ?: 0) - 1) {
                        showInlineError(R.string.mozo_pin_reset_msg_error, hasIncorrectWord).join()
                        mInteractionListener?.getCloseButton()?.isEnabled = !hasIncorrectWord
                    }
                }
            }
        }
    }

    private fun finalVerify() {
        binding.resetPinLoadingView.visible()
        mInteractionListener?.run {
            hideToolbarActions(left = true, right = false)
            getCloseButton()?.setText(R.string.mozo_button_cancel)
        }
        isCancelableRightButton = true

        val mnemonic = mInputView?.map { it.text.toString().trim() }?.joinToString(separator = " ") { s -> s }
        if (!mnemonic.isNullOrEmpty() && MnemonicUtils.validateMnemonic(mnemonic)) {
            verifyJob = MozoSDK.scope.launch {
                val wallet = WalletHelper(mnemonic)
                val isCorrectWallet = wallet.buildWalletInfo().offchainAddress
                        .equals(MozoWallet.getInstance().getAddress(), ignoreCase = true)

                /* Prevent crash when back pressed during working */
                if (!isAdded || isRemoving) return@launch

                if (isCorrectWallet) {
                    withContext(Dispatchers.Main) {
                        mInputView?.map { it.text = null }
                    }
                    mInteractionListener?.getResetPinModel()?.setData(wallet)
                    mInteractionListener?.requestEnterPin()
                } else {
                    showInlineError(R.string.mozo_pin_reset_msg_error_incorrect, true)
                }
            }

        } else {
            showInlineError(R.string.mozo_pin_reset_msg_error_incorrect, true)
        }
    }

    private fun showInlineError(@StringRes error: Int, showing: Boolean) = MainScope().launch {
        if (!isAdded || isDetached) return@launch
        binding.resetPinSubTitle.apply {
            setText(
                    if (showing) error
                    else R.string.mozo_pin_reset_sub_title
            )
            isSelected = showing
        }

        binding.resetPinLoadingView.gone()
        mInteractionListener?.run {
            getCloseButton()?.setText(R.string.mozo_button_submit)
            hideToolbarActions(left = false, right = false)
        }
        isCancelableRightButton = false
    }

    override fun onCloseClicked() {
        if (!isVisible) return
        if (isCancelableRightButton) {
            EventBus.getDefault().post(MessageEvent.CloseActivities())

        } else {
            binding.resetPinSubTitle.apply {
                requestFocus()
                hideKeyboard()
                postDelayed(200) {
                    finalVerify()
                }
            }
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