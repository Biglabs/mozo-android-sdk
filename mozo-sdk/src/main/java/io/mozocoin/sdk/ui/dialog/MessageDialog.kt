package io.mozocoin.sdk.ui.dialog

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.string
import kotlinx.android.synthetic.main.dialog_message.*

class MessageDialog(context: Context, val message: String) : BaseDialog(context) {

    private var buttonText: String? = null
    private var buttonAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_message)

        dialog_msg_content.text = message

        buttonText?.let {
            button_close.text = it
        }
        button_close.click {
            dismiss()
            buttonAction?.invoke()
        }
    }

    fun setAction(@StringRes button: Int, buttonClicked: (() -> Unit)? = null) =
            setAction(context.string(button), buttonClicked)

    fun setAction(button: String, buttonClicked: (() -> Unit)? = null): MessageDialog {
        buttonText = button
        buttonAction = buttonClicked
        return this
    }

    fun cancelable(flag: Boolean): MessageDialog {
        setCancelable(flag)
        return this
    }

    companion object {
        fun show(context: Context, @StringRes message: Int) {
            show(context, context.getString(message))
        }

        fun show(context: Context, message: String) = synchronized(this) {
            MessageDialog(context, message).show()
        }
    }
}