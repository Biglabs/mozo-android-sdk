package com.biglabs.mozo.sdk.ui.widget

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.auth.MozoAuth
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.trans.TransactionHistoryActivity

class TransactionHistoryButton : BaseButton {

    private var needToContinue = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_transaction_history)

        val icon = ContextCompat.getDrawable(context, R.drawable.ic_btn_history)
        super.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }

    override fun authorizeChanged(auth: MessageEvent.Auth) {
        if (needToContinue && MozoAuth.getInstance().isSignUpCompleted()) {
            needToContinue = false
            doOpenTxHistory()
        }
    }

    private fun doOpenTxHistory() {
        TransactionHistoryActivity.start(context)
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().run {
            if (isSignUpCompleted())
                doOpenTxHistory()
            else {
                needToContinue = true
                signIn()
            }
        }
    }
}