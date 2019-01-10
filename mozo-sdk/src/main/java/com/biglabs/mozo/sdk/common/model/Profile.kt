package com.biglabs.mozo.sdk.common.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["id", "userId"], unique = true)])
data class Profile(
        @PrimaryKey
        var id: Long = 0L,
        val userId: String,
        val status: String? = null,
        @Embedded
        var exchangeInfo: ExchangeInfo? = null,
        @Embedded
        val settings: Settings? = null,
        @Embedded
        var walletInfo: WalletInfo? = null
)