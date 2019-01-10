package com.biglabs.mozo.sdk.common.model

import com.biglabs.mozo.sdk.common.Constant

data class TransactionStatus(
        val txHash: String,
        val status: String
) {
    fun isSuccess() = Constant.STATUS_SUCCESS.equals(status, ignoreCase = true)
    fun isFailed() = Constant.STATUS_FAILED.equals(status, ignoreCase = true)
}