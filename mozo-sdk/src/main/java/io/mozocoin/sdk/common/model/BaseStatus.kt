package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.common.Constant

open class BaseStatus {
    var status: String = Constant.STATUS_FAILED

    fun isSuccess() = Constant.STATUS_SUCCESS.equals(status, ignoreCase = true)
    fun isFailed() = Constant.STATUS_FAILED.equals(status, ignoreCase = true)
}