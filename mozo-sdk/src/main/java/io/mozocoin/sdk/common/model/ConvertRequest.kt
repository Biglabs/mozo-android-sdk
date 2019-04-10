package io.mozocoin.sdk.common.model

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class ConvertRequest(
        val fromAddress: String,
        val gasLimit: BigDecimal,
        val gasPrice: BigDecimal,
        val toAddress: String,
        val value: BigDecimal,
        @Ignore @Transient var gasPriceProgress: Int = 0
) : Parcelable