package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.utils.safe
import java.math.BigDecimal

data class ExchangeRateInfo(
        val currency: String?,
        val currencySymbol: String?,
        val rate: BigDecimal?
) {
    fun rate() = rate.safe()
}