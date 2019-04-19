package io.mozocoin.sdk.authentication

abstract class AuthStateListener {
    open fun onAuthStateChanged(singedIn: Boolean) {}
    open fun onAuthCanceled() {}
    open fun onAuthFailed() {}
}