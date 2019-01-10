package com.biglabs.mozo.sdk.transaction.payment

import com.biglabs.mozo.sdk.common.model.PaymentRequest

interface PaymentRequestInteractionListener {
    fun onCreateRequestClicked(amount: String)
    fun onSendRequestClicked(amount: String, toAddress: String, request: PaymentRequest)
}