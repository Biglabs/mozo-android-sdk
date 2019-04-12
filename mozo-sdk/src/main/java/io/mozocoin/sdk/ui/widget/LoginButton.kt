package io.mozocoin.sdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R

internal class LoginButton : BaseButton {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_login)
    }

    override fun buttonIcon(): Int = R.drawable.ic_action_auth

    override fun authorizeChanged(isSignUpCompleted: Boolean) {
        super.setText(if (isSignUpCompleted) R.string.mozo_button_logout else R.string.mozo_button_login)
        isActivated = isSignUpCompleted
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().run {
            if (isSignedIn()) signOut() else signIn()
        }
    }
}