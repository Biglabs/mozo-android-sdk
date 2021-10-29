package io.mozocoin.sdk.common.model

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName
import io.mozocoin.sdk.utils.equalsIgnoreCase

@Suppress("SpellCheckingInspection")
data class WalletInfo constructor(
    var encryptSeedPhrase: String? = null,
    var offchainAddress: String? = null,
    var onchainAddress: String? = null,
    @SerializedName("encryptedPin")
    var pin: String? = null
) {

    @Ignore // Add this for suppress warning: There are multiple good constructors and Room will pick the no-arg constructor
    constructor() : this(null, null, null, null)

    override fun equals(other: Any?): Boolean {
        if (other is WalletInfo) {
            return encryptSeedPhrase == other.encryptSeedPhrase &&
                    offchainAddress?.equalsIgnoreCase(other.offchainAddress) == true &&
                    !other.onchainAddress.isNullOrEmpty()
            /**
             * Should be fully verify this.onchainAddress == other.onchainAddress
             * but unnecessary for now
             */
        }
        return false
    }

    override fun hashCode(): Int {
        var result = encryptSeedPhrase?.hashCode() ?: 0
        result = 31 * result + (offchainAddress?.hashCode() ?: 0)
        result = 31 * result + (onchainAddress?.hashCode() ?: 0)
        return result
    }
}