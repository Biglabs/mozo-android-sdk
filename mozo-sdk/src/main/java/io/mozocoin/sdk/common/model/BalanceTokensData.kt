package io.mozocoin.sdk.common.model

data class BalanceTokensData(
        val balanceOfTokenOffchain: BalanceInfo?,
        val balanceOfTokenOnchain: BalanceInfo?,
        val convertToMozoXOnchain: Boolean,
        val detectedOnchain: Boolean
)