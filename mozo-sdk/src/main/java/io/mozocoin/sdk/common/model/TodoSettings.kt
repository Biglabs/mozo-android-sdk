package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName

data class TodoSettings(
        @SerializedName("colors")
        var colors: Map<String, String>? = null
)