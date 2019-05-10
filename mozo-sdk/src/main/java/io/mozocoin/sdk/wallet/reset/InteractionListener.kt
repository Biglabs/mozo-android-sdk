package io.mozocoin.sdk.wallet.reset

import android.widget.TextView

internal interface InteractionListener {
    fun getCloseButton(): TextView?

    fun getResetPinModel(): ResetPinViewModel

    fun requestEnterPin()
}