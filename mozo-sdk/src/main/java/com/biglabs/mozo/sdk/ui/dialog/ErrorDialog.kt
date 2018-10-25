package com.biglabs.mozo.sdk.ui.dialog

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.annotation.IntDef
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_error.*

internal class ErrorDialog(context: Context, private val argument: Bundle, private val onTryAgain: (() -> Unit)? = null) : BaseDialog(context) {

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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        instance = null
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

        fun generalError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_GENERAL, onTryAgain)
        }

        fun networkError(context: Context?, onTryAgain: (() -> Unit)? = null) {
            show(context, TYPE_NETWORK, onTryAgain)
        }

        fun show(context: Context?, @ErrorType type: Int, onTryAgain: (() -> Unit)? = null) = synchronized(this) {
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
    }
}