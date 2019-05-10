package io.mozocoin.sdk.wallet.reset

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.widget.onBackPress
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_reset_enter_pin.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        mInteractionListener?.getCloseButton()?.apply {
            setText(R.string.mozo_button_cancel)
            isEnabled = true
            visible()
        }

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

    override fun onCloseClicked() {
        if (!isVisible) return


    }

    private fun showConfirmUI() {
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_confirm_sub_title)
        reset_pin_enter_pin_sub_content?.setText(R.string.mozo_pin_confirm_content)
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

    private fun submit() {
        reset_pin_loading_view?.visible()

        val data = mInteractionListener?.getResetPinModel()?.getData()
        GlobalScope.launch {
            data?.encrypt(mPinEntering)
            mInteractionListener?.getResetPinModel()?.setData(data)

            MozoAPIsService.getInstance().resetWallet(
                    context ?: return@launch,
                    data?.buildWalletInfo() ?: return@launch,
                    { data, errorCode ->

                    },
                    this@EnterPinFragment::submit
            )
        }
    }

    companion object {
        fun newInstance() = EnterPinFragment()
    }
}