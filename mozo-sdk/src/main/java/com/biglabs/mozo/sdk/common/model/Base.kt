package com.biglabs.mozo.sdk.common.model

import com.google.gson.annotations.SerializedName

class Base<T> {
    @SerializedName("success")
    val isSuccess: Boolean = false

    @SerializedName("error")
    var errorCode: String? = null

    @SerializedName("data")
    var data: T? = null
}