package com.biglabs.mozo.sdk.common.model

import com.google.gson.annotations.SerializedName

class BaseData<T> {
    @SerializedName("items")
    var items: List<T>? = null

    @SerializedName("totalItems")
    var totalItems: Int = 0
}