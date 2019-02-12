package io.mozocoin.sdk.common.model

data class ExchangeInfo(
        val apiKey: String,
        val depositAddress: String? = null,
        val exchangeId: String? = null,
        val exchangePlatform: String? = null,
        var exchangeSecret: String? = null
)