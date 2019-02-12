package io.mozocoin.sdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.transaction.TransactionHistoryActivity

internal class TransactionHistoryButton : BaseButton {

    private var needToContinue = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_transaction_history)
    }

    override fun buttonIcon(): Int = R.drawable.ic_action_time

    override fun authorizeChanged(signedIn: Boolean) {
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