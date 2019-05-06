package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class BroadcastDataContent(
        val event: String?,
        val from: String,
        val to: String,
        val amount: BigDecimal?,
        val decimal: Int,
        val symbol: String,
        var time: Long,
        @SerializedName(value = "phoneNo", alternate = ["phoneNumSignUp"]) val phoneNo: String?,
        val isComeIn: Boolean,
        val storeName: String
) {
    override fun toString(): String =
            "{event=$event, from=$from, to=$to, amount=$amount, decimal=$decimal, symbol=$symbol, time=$time, phoneNo=$phoneNo, comeIn=$isComeIn, storeName=$storeName}"
}