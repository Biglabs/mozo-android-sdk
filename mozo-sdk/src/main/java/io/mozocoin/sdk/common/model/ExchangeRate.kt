package io.mozocoin.sdk.common.model

data class ExchangeRate(
        val currency: String?,
        val currencySymbol: String?,
        val rate: Double
)