package com.biglabs.mozo.sdk.common.model

data class TransactionRequest(
        val inputs: ArrayList<TransactionAddress> = arrayListOf(),
        val outputs: ArrayList<TransactionAddressOutput> = arrayListOf()
)