package com.biglabs.mozo.sdk.common.model

import com.biglabs.mozo.sdk.utils.Support
import java.math.BigDecimal

data class BalanceInfo(
        val balance: BigDecimal,
        val symbol: String?,
        val decimals: Int,
        val contractAddress: String?
) {
    fun balanceNonDecimal(): BigDecimal = Support.toAmountNonDecimal(balance, decimals)
}