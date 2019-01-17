package com.biglabs.mozo.sdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.R

internal class LoginButton : BaseButton {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setText(R.string.mozo_button_login)
    }

    override fun buttonIcon(): Int = R.drawable.ic_action_login

    override fun authorizeChanged(signedIn: Boolean) {
        if (signedIn) {
            setIconResource(R.drawable.ic_action_logout)
            super.setText(R.string.mozo_button_logout)
        } else {
            setIconResource(R.drawable.ic_action_login)
            super.setText(R.string.mozo_button_login)
        }
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().run {
            if (isSignedIn()) signOut() else signIn()
        }
    }
}