package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.utils.Support
import java.math.BigDecimal

data class BalanceInfo(
        val balance: BigDecimal,
        val symbol: String?,
        val decimals: Int,
        val contractAddress: String?
) {
    fun balanceNonDecimal(): BigDecimal = Support.toAmountNonDecimal(balance, decimals)
}