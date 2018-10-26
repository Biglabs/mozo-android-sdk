package com.biglabs.mozo.sdk.authentication

import android.support.annotation.CallSuper

abstract class AuthenticationListener {
    @CallSuper
    open fun onChanged(isSinged: Boolean) {
        if (isSinged) onSignedIn() else onSignedOut()
    }

    open fun onSignedIn() {}
    open fun onSignedOut() {}
}