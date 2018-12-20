package com.biglabs.mozo.sdk.transaction.payment

import com.biglabs.mozo.sdk.common.Models

interface PaymentRequestInteractionListener {
    fun onCreateRequestClicked(amount: String)
    fun onSendRequestClicked(amount: String, toAddress: String, request: Models.PaymentRequest)
}