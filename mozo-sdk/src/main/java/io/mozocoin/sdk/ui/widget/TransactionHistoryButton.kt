package io.mozocoin.sdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.R

internal class TransactionHistoryButton : BaseButton {

    private var needToContinue = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_transaction_history)
    }

    override fun buttonIcon(): Int = R.drawable.ic_action_time

    override fun authorizeChanged(isSignUpCompleted: Boolean) {
        if (needToContinue) {
            needToContinue = false
            if (isSignUpCompleted) doOpenTxHistory()
        }
    }

    private fun doOpenTxHistory() {
        MozoTx.getInstance().openTransactionHistory(context)
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().isSignUpCompleted(view.context) {
            if (it) doOpenTxHistory()
            else {
                needToContinue = true
                MozoAuth.getInstance().signIn()
            }
        }
    }
}