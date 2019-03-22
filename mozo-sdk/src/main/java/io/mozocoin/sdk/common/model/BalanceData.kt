package io.mozocoin.sdk.common.model

data class BalanceData(
        val balanceOfETH: BalanceInfo?,
        val balanceOfToken: BalanceInfo?,
        val convertToMozoXOnchain: Boolean
)