package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName

data class Todo(
        @SerializedName("id")
        val id: String? = null,

        @SerializedName("severity")
        val severity: String? = null
)