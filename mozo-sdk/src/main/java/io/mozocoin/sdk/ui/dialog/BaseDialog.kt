package io.mozocoin.sdk.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import io.mozocoin.sdk.R

open class BaseDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val value = TypedValue()
        context.resources.getValue(R.dimen.mozo_background_dim_amount, value, true)

        window?.apply {
            setBackgroundDrawableResource(R.drawable.mozo_bg_dialog_error)
            setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(value.float)
        }
    }
}