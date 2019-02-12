package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.common.Constant

data class TransactionStatus(
        val txHash: String,
        val status: String
) {
    fun isSuccess() = Constant.STATUS_SUCCESS.equals(status, ignoreCase = true)
    fun isFailed() = Constant.STATUS_FAILED.equals(status, ignoreCase = true)
}