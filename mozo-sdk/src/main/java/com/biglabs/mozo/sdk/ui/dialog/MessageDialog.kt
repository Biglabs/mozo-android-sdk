package com.biglabs.mozo.sdk.ui.dialog

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_message.*

class MessageDialog(context: Context, val message: String) : BaseDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_message)

        dialog_msg_content.text = message
        button_close.click {
            dismiss()
        }
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