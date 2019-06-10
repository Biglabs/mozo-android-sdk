package io.mozocoin.sdk.common.model

@Suppress("SpellCheckingInspection")
data class WalletInfo(
        var encryptSeedPhrase: String? = null,
        var offchainAddress: String? = null,
        var onchainAddress: String? = null,
        var pin: String? = null
)