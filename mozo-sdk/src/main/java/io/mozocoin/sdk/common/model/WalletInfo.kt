package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName

@Suppress("SpellCheckingInspection")
data class WalletInfo(
        var encryptSeedPhrase: String? = null,
        var offchainAddress: String? = null,
        var onchainAddress: String? = null,
        @SerializedName("encryptedPin")
        var pin: String? = null
)