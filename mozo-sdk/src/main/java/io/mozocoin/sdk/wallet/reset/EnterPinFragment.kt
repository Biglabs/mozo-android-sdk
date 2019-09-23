package io.mozocoin.sdk.wallet.reset

import android.app.Activity.RESULT_OK
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.core.view.setPadding
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.common.service.NetworkSchedulerService
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_reset_enter_pin.*
import kotlinx.android.synthetic.main.view_message_progress_status.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class EnterPinFragment : ResetPinBaseFragment() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_reset_enter_pin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reset_pin_enter_pin_input?.apply {
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
                            reset_pin_enter_pin_input?.text = null
                            showPinInputWrongUI()
                        }
                    }
                }
            }

            GlobalScope.launch(Dispatchers.Main) {
                delay(500L)
                showKeyboard()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
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
        }, 500)
    }

    override fun onCloseClicked() {
        if (!isVisible) return

        EventBus.getDefault().post(MessageEvent.CloseActivities())
    }

    private fun resetInputUI() {
        mPinEntering = ""
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_reset_header_create)
        reset_pin_enter_pin_sub_content?.setText(R.string.mozo_pin_content)
        reset_pin_enter_pin_input?.text = null
        text_correct_pin?.gone()
        text_incorrect_pin?.gone()
        mInteractionListener?.hideToolbarActions(left = true, right = false)
    }

    private fun showConfirmUI() {
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_confirm_sub_title)
        reset_pin_enter_pin_sub_content?.setText(R.string.mozo_pin_confirm_content)
        reset_pin_enter_pin_input?.text = null
    }

    private fun showPinInputCorrectUI() {
        text_correct_pin?.visible()
        text_incorrect_pin?.gone()
    }

    private fun showPinInputWrongUI() {
        text_correct_pin?.gone()
        text_incorrect_pin?.visible()
    }

    private fun hidePinInputWrongUI() {
        text_incorrect_pin?.gone()
    }

    private fun submit() = GlobalScope.launch(Dispatchers.Main) {
        delay(700)

        reset_pin_enter_pin_input?.isEnabled = false
        reset_pin_loading_view?.visible()
        reset_pin_message_view?.gone()
        mInteractionListener?.hideToolbarActions(left = true, right = true)

        val data = mInteractionListener?.getResetPinModel()?.getData()
        withContext(Dispatchers.Default) {
            data?.encrypt(mPinEntering)
            mInteractionListener?.getResetPinModel()?.setData(data)

            if (!NetworkSchedulerService.isNetworkAvailable()) {
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

                MozoAuth.getInstance().saveUserInfo(context ?: return@resetWallet, profile, data) {
                    showMessage(if (it) MESSAGE_SUCCESS else MESSAGE_ERROR_COMMON)
                }
            }
        }
    }

    private fun showMessage(@IntRange(from = MESSAGE_SUCCESS, to = MESSAGE_ERROR_COMMON) type: Long) {
        GlobalScope.launch(Dispatchers.Main) {
            var icon = R.drawable.ic_error_general
            var title = R.string.mozo_dialog_error_msg
            var buttonText = R.string.mozo_button_try_again
            var buttonClickCallback: (View) -> Unit = {
                resetInputUI()
                activity?.onBackPressed()
            }
            view_message_icon?.setPadding(0)
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
                    view_message_icon?.setPadding(resources.dp2Px(16f).toInt())
                    mInteractionListener?.hideToolbarActions(left = true, right = true)
                }
                MESSAGE_ERROR_NETWORK -> {
                    icon = R.drawable.ic_error_network
                    title = R.string.mozo_dialog_error_network_msg
                }
            }

            view_message_icon?.setImageResource(icon)
            view_message_title?.setText(title)
            view_message_retry_btn?.setText(buttonText)
            view_message_retry_btn?.click(buttonClickCallback)
            reset_pin_message_view?.visible()
        }
    }

    companion object {
        private const val MESSAGE_SUCCESS = 0L
        private const val MESSAGE_ERROR_NETWORK = 1L
        private const val MESSAGE_ERROR_COMMON = 2L

        fun newInstance() = EnterPinFragment()
    }
}