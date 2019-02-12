package io.mozocoin.sdk.transaction.payment

import io.mozocoin.sdk.common.model.PaymentRequest

interface PaymentRequestInteractionListener {
    fun onCreateRequestClicked(amount: String)
    fun onSendRequestClicked(amount: String, toAddress: String, request: PaymentRequest)
}