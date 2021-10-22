package io.mozocoin.sdk.common.model

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ConvertRequest(
        val fromAddress: String,
        val gasLimit: BigDecimal,
        val gasPrice: BigDecimal,
        val toAddress: String,
        val value: BigDecimal,
        @Ignore @Transient var gasPriceProgress: Int = 0,
        @Ignore @Transient var on2Off: Boolean = true
) : Parcelable