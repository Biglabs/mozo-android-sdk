package io.mozocoin.sdk.common.model

import androidx.room.*

@Entity(indices = [Index(value = ["id", "userId"], unique = true)])
data class Profile(
        @PrimaryKey
        var id: Long = 0L,
        var userId: String? = null,
        @Ignore val avatarUrl: String? = null,
        @Ignore val fullName: String? = null,
        @Ignore val phoneNumber: String? = null,
        @Ignore val birthday: Long = 0L,
        @Ignore val email: String? = null,
        @Ignore val gender: String? = null,
        var status: String? = null,
        @Embedded var exchangeInfo: ExchangeInfo? = null,
        @Embedded var settings: Settings? = null,
        @Embedded var walletInfo: WalletInfo? = null
)