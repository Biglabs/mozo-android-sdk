package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class BroadcastDataContent(
        val title: String?,
        val body: String?,
        @SerializedName("data") val contact: Contact?,
        val event: String?,
        val from: String,
        val to: String,
        val amount: BigDecimal?,
        val decimal: Int,
        val symbol: String,
        var time: Long,
        @SerializedName(value = "phoneNo", alternate = ["phoneNumSignUp"]) val phoneNo: String?,
        val isComeIn: Boolean,
        val storeName: String,
        val promoName: String? = null,
        var imageId: String? = null
)