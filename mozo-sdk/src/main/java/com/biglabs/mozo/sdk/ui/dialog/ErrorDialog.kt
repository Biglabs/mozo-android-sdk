package com.biglabs.mozo.sdk.ui.dialog

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.IntDef
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_error.*
import org.greenrobot.eventbus.EventBus

class ErrorDialog(context: Context, private val argument: Bundle, private val onTryAgain: (() -> Unit)? = null) : BaseDialog(context) {

    private var errorType = TYPE_GENERAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_error)

        errorType = argument.getInt(ERROR_TYPE, errorType)

        when (errorType) {
            TYPE_GENERAL -> {
                image_error_type.setImageResource(R.drawable.ic_error_general)
                text_msg_error.setText(R.string.mozo_dialog_error_msg)
            }
            TYPE_NETWORK -> {
                image_error_type.setImageResource(R.drawable.ic_error_network)
                text_msg_error.setText(R.string.mozo_dialog_error_network_msg)
            }
        }

        button_try_again.click {
            onTryAgain?.invoke()
            dismiss()
        }
    }

    override fun cancel() {
        super.cancel()
        cancelCallback?.onCancel(this)
        EventBus.getDefault().post(MessageEvent.UserCancelErrorDialog())
    }

    override fun dismiss() {
        super.dismiss()
        dismissCallback?.onDismiss(this)
    }

    override fun onStop() {
        super.onStop()
        instance = null
        cancelCallback = null
        dismissCallback = null
    }

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(TYPE_GENERAL, TYPE_NETWORK)
        annotation class ErrorType

        const val TYPE_GENERAL = 0
        const val TYPE_NETWORK = 1

        private const val ERROR_TYPE = "ERROR_TYPE"

        @Volatile
        private var instance: ErrorDialog? = null

        @Volatile
        private var dismissCallback: DialogInterface.OnDismissListener? = null

        @Volatile
        private var cancelCallback: DialogInterface.OnCancelListener? = null

        fun generalError(context: Context?, forceShow: Boolean = false, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_GENERAL, forceShow, onTryAgain)
        }

        fun networkError(context: Context?, forceShow: Boolean = false, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_NETWORK, forceShow, onTryAgain)
        }

        fun show(context: Context?, @ErrorType type: Int, forceShow: Boolean = false, onTryAgain: (() -> Unit)? = null) = synchronized(this) {
            if (!forceShow && isShowing()) return
            context?.run {
                if (this is Activity && (isFinishing || isDestroyed)) return@synchronized
                instance?.apply {
                    dismiss()
                }

                val bundle = Bundle()
                bundle.putInt(ERROR_TYPE, type)

                instance = ErrorDialog(this, bundle, onTryAgain)
                instance!!.show()
            }
        }

        fun onDismiss(callback: DialogInterface.OnDismissListener) {
            dismissCallback = callback
        }

        fun onCancel(callback: DialogInterface.OnCancelListener) {
            cancelCallback = callback
        }

        fun isShowing() = instance?.isShowing == true
    }
}