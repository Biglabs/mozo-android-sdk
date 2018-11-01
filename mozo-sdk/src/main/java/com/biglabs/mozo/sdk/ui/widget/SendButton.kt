package com.biglabs.mozo.sdk.ui.widget

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoTx

internal class SendButton : BaseButton {

    private var needToContinue = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_transfer)

        val icon = ContextCompat.getDrawable(context, R.drawable.ic_btn_send)
        super.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }

    override fun authorizeChanged(signedIn: Boolean) {
        if (needToContinue && MozoAuth.getInstance().isSignUpCompleted()) {
            needToContinue = false
            doTransfer()
        }
    }

    private fun doTransfer() {
        MozoTx.getInstance().transfer()
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().run {
            if (isSignUpCompleted())
                doTransfer()
            else {
                needToContinue = true
                signIn()
            }
        }
    }
}