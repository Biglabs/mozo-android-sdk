package io.mozocoin.sdk.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import io.mozocoin.sdk.databinding.DialogMessageBinding
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.string

class MessageDialog(context: Context, val message: CharSequence) : BaseDialog(context) {

    private lateinit var binding: DialogMessageBinding
    private var buttonText: String? = null
    private var buttonAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogMsgContent.text = message

        buttonText?.let {
            binding.buttonClose.text = it
        }
        binding.buttonClose.click {
            dismiss()
            buttonAction?.invoke()
        }
    }

    override fun show() {
        try {
            if (instance?.isShowing == true)
                instance?.dismiss()
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        super.show()

        val ctx = context
        if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) return
        instance = this
    }

    override fun onStop() {
        super.onStop()
        instance = null
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
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MessageDialog? = null

        fun show(context: Context, @StringRes message: Int) {
            show(context, context.getString(message))
        }

        fun show(context: Context, message: CharSequence) = synchronized(this) {
            MessageDialog(context, message).show()
        }

        fun dismiss() {
            instance?.dismiss()
            instance = null
        }
    }
}