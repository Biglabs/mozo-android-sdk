package io.mozocoin.sdk.common.model

import android.os.Parcelable
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.safe
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class BalanceInfo(
        val balance: BigDecimal?,
        val symbol: String?,
        val decimals: Int?,
        val contractAddress: String?
) : Parcelable {
    fun balanceNonDecimal(): BigDecimal = Support.toAmountNonDecimal(
            balance.safe(),
            decimals ?: Constant.DEFAULT_DECIMAL
    )
}