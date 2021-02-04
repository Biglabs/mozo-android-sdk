package io.mozocoin.sdk.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.IntDef
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.ConnectionService
import io.mozocoin.sdk.databinding.DialogErrorBinding
import io.mozocoin.sdk.ui.MozoSnackbar
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.visible
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class ErrorDialog(context: Context, private val argument: Bundle) : BaseDialog(context) {

    private lateinit var binding: DialogErrorBinding
    private var errorType = TYPE_GENERAL
    private var errorMessage: String? = null
    private var identifier: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonContactTelegram.click {
            MozoSDK.contactTelegram(context)
        }

        binding.buttonContactZalo.click {
            MozoSDK.contactZalo(context)
        }

        binding.buttonContactKakao.click {
            MozoSDK.contactKaKaoTalk(context)
        }

        binding.buttonTryAgain.click {
            retry()
        }

        updateUI()
    }

    override fun cancel() {
        cancelCallback?.onCancel(this)
        EventBus.getDefault().post(MessageEvent.UserCancelErrorDialog())
        ConnectionService.checkNetwork()
        super.cancel()
    }

    override fun dismiss() {
        try {
            MozoSDK.getInstance().retryCallbacks?.clear()
            dismissCallback?.onDismiss(this)
            super.dismiss()
        } catch (ignore: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        instance = null
        MozoSDK.getInstance().retryCallbacks = null
        cancelCallback = null
        dismissCallback = null
    }

    private fun updateUI(@ErrorType type: Int? = null) {

        errorType = type ?: argument.getInt(ERROR_TYPE, errorType)
        errorMessage = argument.getString(ERROR_MESSAGE)

        visible(
                binding.imageErrorType,
                binding.textTitleError
        )
        gone(
                binding.buttonContactTelegram,
                binding.buttonContactZalo,
                binding.buttonContactKakao
        )
        binding.buttonTryAgain.setText(R.string.mozo_button_try_again)

        when (errorType) {
            TYPE_GENERAL -> {
                binding.imageErrorType.setImageResource(R.drawable.ic_error_general)
                binding.textTitleError.setText(R.string.mozo_dialog_error_msg)
            }
            TYPE_NETWORK -> {
                binding.imageErrorType.setImageResource(R.drawable.ic_error_network)
                binding.textTitleError.setText(R.string.mozo_dialog_error_network_msg)
                binding.textMsgError.setText(R.string.mozo_dialog_error_network_msg_sub)
                binding.textMsgError.visible()
            }
            TYPE_TIMEOUT -> {
                binding.imageErrorType.setImageResource(R.drawable.ic_error_timeout)
                binding.textTitleError.setText(R.string.mozo_dialog_error_timeout_msg)
            }
            TYPE_WITH_CONTACT -> {
                gone(
                        binding.imageErrorType,
                        binding.textTitleError
                )
                visible(
                        binding.textMsgError,
                        binding.buttonContactTelegram,
                        binding.buttonContactZalo,
                        binding.buttonContactKakao
                )
                binding.textMsgError.setText(R.string.error_fatal)
                binding.buttonTryAgain.setText(R.string.mozo_button_ok)
            }
            TYPE_DEACTIVATED -> {
                gone(
                        binding.imageErrorType,
                        binding.textTitleError
                )
                visible(
                        binding.textMsgError,
                        binding.buttonContactTelegram,
                        binding.buttonContactZalo,
                        binding.buttonContactKakao
                )
                binding.textMsgError.setText(R.string.error_account_deactivated)
                binding.buttonTryAgain.setText(R.string.mozo_button_ok)
            }
        }

        errorMessage?.let {
            binding.textMsgError.text = it
        }

        if (errorType == TYPE_NETWORK) {
            MozoSnackbar.dismiss()
        }
    }

    private fun retry() {
        MozoSDK.getInstance().retryCallbacks?.forEach {
            it.invoke()
        }
        dismiss()
    }

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(TYPE_GENERAL, TYPE_NETWORK, TYPE_TIMEOUT, TYPE_WITH_CONTACT, TYPE_DEACTIVATED)
        annotation class ErrorType

        const val TYPE_GENERAL = 0
        const val TYPE_NETWORK = 1
        const val TYPE_TIMEOUT = 2
        const val TYPE_WITH_CONTACT = 3
        const val TYPE_DEACTIVATED = 4

        const val ERROR_TYPE = "ERROR_TYPE"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ErrorDialog? = null

        @Volatile
        private var dismissCallback: DialogInterface.OnDismissListener? = null

        @Volatile
        private var cancelCallback: DialogInterface.OnCancelListener? = null

        @JvmStatic
        fun deactivatedError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_DEACTIVATED, onTryAgain)
        }

        @JvmStatic
        fun generalError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_GENERAL, onTryAgain)
        }

        @JvmStatic
        fun networkError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_NETWORK, onTryAgain)
        }

        @JvmStatic
        fun timeoutError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_TIMEOUT, onTryAgain)
        }

        @JvmStatic
        fun withContactError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_WITH_CONTACT, onTryAgain)
        }

        @JvmStatic
        @Synchronized
        fun show(context: Context?, @ErrorType type: Int, onRetry: (() -> Unit)? = null) = synchronized(this) {
            if (instance?.identifier != context?.toString()) {
                dismiss()
            }
            context?.run {
                if (this is Activity && (isFinishing || isDestroyed)) return@synchronized

                if (MozoSDK.getInstance().retryCallbacks == null) {
                    MozoSDK.getInstance().retryCallbacks = arrayListOf()
                }
                onRetry?.let { MozoSDK.getInstance().retryCallbacks?.add(it) }

                when {
                    instance?.isShowing == true -> instance?.updateUI(type)
                    this !is Application -> {
                        val bundle = Bundle()
                        bundle.putInt(ERROR_TYPE, type)
                        instance = ErrorDialog(this, bundle)
                        instance!!.identifier = this.toString()
                        instance!!.show()
                    }
                    else -> {
                    }
                }
            }
        }

        fun dismiss() {
            instance?.dismiss()
        }

        @JvmStatic
        fun onDismiss(callback: DialogInterface.OnDismissListener) {
            dismissCallback = callback
        }

        @JvmStatic
        fun onCancel(callback: DialogInterface.OnCancelListener) {
            cancelCallback = callback
        }

        fun isShowing() = instance?.isShowing == true

        fun isShowingForNetwork() = instance?.isShowing == true && instance?.errorType == TYPE_NETWORK

        fun setCancelable(cancel: Boolean) {
            instance?.setCancelable(cancel)
        }

        internal fun retry() = MainScope().launch {
            instance?.retry()
        }
    }
}