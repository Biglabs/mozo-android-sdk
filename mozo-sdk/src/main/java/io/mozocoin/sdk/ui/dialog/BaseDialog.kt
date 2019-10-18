package io.mozocoin.sdk.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import io.mozocoin.sdk.R

open class BaseDialog(private val ctx: Context) : Dialog(ctx) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        } catch (ignore: Exception) {
            ignore.printStackTrace()
        }

        val value = TypedValue()
        context.resources.getValue(R.dimen.mozo_background_dim_amount, value, true)

        window?.apply {
            setBackgroundDrawableResource(R.drawable.mozo_bg_dialog_error)
            setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(value.float)
        }
    }

    override fun show() {
        if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) return

        super.show()
    }
}