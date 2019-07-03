package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName

@Suppress("SpellCheckingInspection")
data class WalletInfo(
        var encryptSeedPhrase: String? = null,
        var offchainAddress: String? = null,
        var onchainAddress: String? = null,
        @SerializedName("encryptedPin")
        var pin: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other is WalletInfo) {
            return encryptSeedPhrase == other.encryptSeedPhrase &&
                    offchainAddress == other.offchainAddress &&
                    onchainAddress == other.onchainAddress
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = encryptSeedPhrase?.hashCode() ?: 0
        result = 31 * result + (offchainAddress?.hashCode() ?: 0)
        result = 31 * result + (onchainAddress?.hashCode() ?: 0)
        return result
    }
}