package com.biglabs.mozo.sdk.common.model

import com.google.gson.annotations.SerializedName

data class TransactionResponseData(
        val hash: String? = null,
        val fees: Double,
        val inputs: ArrayList<TransactionAddress>,
        val outputs: ArrayList<TransactionAddressOutput>,
        val data: String,
        @SerializedName("double_spend")
        val doubleSpend: Boolean,
        @SerializedName("gas_price")
        val gasPrice: Double,
        @SerializedName("gas_limit")
        val gasLimit: Double
)