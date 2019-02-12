package io.mozocoin.sdk.authentication

import androidx.annotation.CallSuper

abstract class AuthenticationListener {
    @CallSuper
    open fun onChanged(isSinged: Boolean) {
        if (isSinged) onSignedIn() else onSignedOut()
    }

    open fun onSignedIn() {}
    open fun onSignedOut() {}
    open fun onCanceled() {}
}