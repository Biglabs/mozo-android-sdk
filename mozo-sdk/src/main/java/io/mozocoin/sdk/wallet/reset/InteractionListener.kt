package io.mozocoin.sdk.wallet.reset

import android.widget.TextView

internal interface InteractionListener {
    fun getCloseButton(): TextView?

    fun getResetPinModel(): ResetPinViewModel

    fun hideToolbarActions(left: Boolean, right: Boolean)

    fun requestEnterPin()
}