package com.biglabs.mozo.sdk.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.auth.MozoAuth

class LoginButton : BaseButton {

    private val icSignIn: Drawable?
    private val icSignOut: Drawable?

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        icSignIn = ContextCompat.getDrawable(context, R.drawable.ic_btn_sign_in)
        icSignOut = ContextCompat.getDrawable(context, R.drawable.ic_btn_sign_out)

        if (isInEditMode) {
            super.setCompoundDrawablesWithIntrinsicBounds(icSignIn, null, null, null)
            super.setText(R.string.mozo_button_login)
        } else updateUI()
    }

    override fun authorizeChanged(auth: MessageEvent.Auth) {
        updateUI()
    }

    override fun onClick(view: View) {
        MozoAuth.getInstance().run {
            if (isSignedIn()) signOut() else signIn()
        }
    }

    private fun updateUI() {
        if (MozoAuth.getInstance().isSignedIn()) {
            super.setCompoundDrawablesWithIntrinsicBounds(icSignOut, null, null, null)
            super.setText(R.string.mozo_button_logout)
        } else {
            super.setCompoundDrawablesWithIntrinsicBounds(icSignIn, null, null, null)
            super.setText(R.string.mozo_button_login)
        }
    }
}