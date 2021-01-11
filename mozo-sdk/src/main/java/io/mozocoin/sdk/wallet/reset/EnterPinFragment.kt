package io.mozocoin.sdk.wallet.reset

import android.app.Activity.RESULT_OK
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.core.view.postDelayed
import androidx.core.view.setPadding
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.WalletHelper
import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.common.service.ConnectionService
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.FragmentResetEnterPinBinding
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class EnterPinFragment : ResetPinBaseFragment() {

    private var _binding: FragmentResetEnterPinBinding? = null
    private val binding get() = _binding!!
    private var mInteractionListener: InteractionListener? = null
    private var pinLength: Int = 0
    private var mPinEntering: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is InteractionListener) {
            mInteractionListener = context
        }
        pinLength = context.getInteger(R.integer.security_pin_length)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResetEnterPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resetPinEnterPinInput.apply {
            onBackPress { activity?.onBackPressed() }
            setMaxLength(pinLength)
            onTextChanged {
                hidePinInputWrongUI()
                it?.toString()?.run {
                    if (this.length < pinLength) return@run

                    when {
                        mPinEntering.isEmpty() -> {
                            mPinEntering = this
                            showConfirmUI()
                        }
                        mPinEntering == this -> {
                            showPinInputCorrectUI()
                            submit()
                        }
                        else -> {
                            binding.resetPinEnterPinInput.text = null
                            showPinInputWrongUI()
                        }
                    }
                }
            }

            MainScope().launch {
                delay(500L)
                showKeyboard()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.root?.postDelayed(500) {
            /* Prevent crash when back pressed during working */
            if (!isAdded || isRemoving) return@postDelayed

            mInteractionListener?.let {
                it.hideToolbarActions(left = true, right = false)
                it.getCloseButton()?.apply {
                    setText(R.string.mozo_button_cancel)
                    isEnabled = true
                    visible()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCloseClicked() {
        if (!isVisible) return

        EventBus.getDefault().post(MessageEvent.CloseActivities())
    }

    private fun resetInputUI() {
        mPinEntering = ""
        binding.resetPinEnterPinHeader.setText(R.string.mozo_pin_reset_header_create)
        binding.resetPinEnterPinSubContent.setText(R.string.mozo_pin_content)
        binding.resetPinEnterPinInput.text = null
        binding.textCorrectPin.gone()
        binding.textIncorrectPin.gone()
        mInteractionListener?.hideToolbarActions(left = true, right = false)
    }

    private fun showConfirmUI() {
        binding.resetPinEnterPinHeader.setText(R.string.mozo_pin_confirm_sub_title)
        binding.resetPinEnterPinSubContent.setText(R.string.mozo_pin_confirm_content)
        binding.resetPinEnterPinInput.text = null
    }

    private fun showPinInputCorrectUI() {
        binding.textCorrectPin.visible()
        binding.textIncorrectPin.gone()
    }

    private fun showPinInputWrongUI() {
        binding.textCorrectPin.gone()
        binding.textIncorrectPin.visible()
    }

    private fun hidePinInputWrongUI() {
        binding.textIncorrectPin.gone()
    }

    private fun submit() = MainScope().launch {
        delay(700)

        binding.resetPinEnterPinInput.isEnabled = false
        binding.resetPinLoadingView.visible()
        binding.resetPinMessageView.root.gone()
        mInteractionListener?.hideToolbarActions(left = true, right = true)

        val data = mInteractionListener?.getResetPinModel()?.getData()
        withContext(Dispatchers.Default) {
            data?.encrypt(mPinEntering)
            mInteractionListener?.getResetPinModel()?.setData(data)

            if (!ConnectionService.isNetworkAvailable) {
                showMessage(MESSAGE_ERROR_NETWORK)
                return@withContext
            }
            MozoAPIsService.getInstance().resetWallet(
                    context ?: return@withContext,
                    data?.buildWalletInfo() ?: return@withContext) { profile, error ->
                if (profile == null) {
                    showMessage(if (error == null) MESSAGE_ERROR_NETWORK else MESSAGE_ERROR_COMMON)
                    return@resetWallet
                }

                updateWallet(profile.walletInfo, data)
            }
        }
    }

    private fun updateWallet(walletInfo: WalletInfo?, walletHelper: WalletHelper?) {
        walletInfo ?: return
        MozoAPIsService.getInstance().updateWalletAfterReset(
                context ?: return,
                walletInfo,
                { profile, error ->

                    if (profile == null) {
                        showMessage(if (error == null) MESSAGE_ERROR_NETWORK else MESSAGE_ERROR_COMMON)
                        return@updateWalletAfterReset
                    }

                    MozoAuth.getInstance().saveUserInfo(context
                            ?: return@updateWalletAfterReset, profile, walletHelper) {
                        showMessage(if (it) MESSAGE_SUCCESS else MESSAGE_ERROR_COMMON)
                    }
                },
                {
                    updateWallet(walletInfo, walletHelper)
                }
        )
    }

    private fun showMessage(@IntRange(from = MESSAGE_SUCCESS, to = MESSAGE_ERROR_COMMON) type: Long) {
        MainScope().launch {
            var icon = R.drawable.ic_error_general
            var title = R.string.mozo_dialog_error_msg
            var buttonText = R.string.mozo_button_try_again
            var buttonClickCallback: (View) -> Unit = {
                resetInputUI()
                activity?.onBackPressed()
            }
            binding.resetPinMessageView.viewMessageIcon.setPadding(0)
            mInteractionListener?.let {
                it.hideToolbarActions(left = true, right = false)
                it.getCloseButton()?.apply {
                    setText(R.string.mozo_button_cancel)
                    isEnabled = true
                    visible()
                }
            }

            when (type) {
                MESSAGE_SUCCESS -> {
                    activity?.setResult(RESULT_OK)
                    icon = R.drawable.ic_check_green
                    title = R.string.mozo_pin_reset_msg_done
                    buttonText = R.string.mozo_button_done
                    buttonClickCallback = {
                        activity?.finish()
                    }
                    binding.resetPinMessageView.viewMessageIcon.setPadding(resources.dp2Px(16f).toInt())
                    mInteractionListener?.hideToolbarActions(left = true, right = true)
                }
                MESSAGE_ERROR_NETWORK -> {
                    icon = R.drawable.ic_error_network
                    title = R.string.mozo_dialog_error_network_msg
                }
            }

            binding.resetPinMessageView.viewMessageIcon.setImageResource(icon)
            binding.resetPinMessageView.viewMessageTitle.setText(title)
            binding.resetPinMessageView.viewMessageRetryBtn.setText(buttonText)
            binding.resetPinMessageView.viewMessageRetryBtn.click(buttonClickCallback)
            binding.resetPinMessageView.root.visible()
        }
    }

    companion object {
        private const val MESSAGE_SUCCESS = 0L
        private const val MESSAGE_ERROR_NETWORK = 1L
        private const val MESSAGE_ERROR_COMMON = 2L

        fun newInstance() = EnterPinFragment()
    }
}