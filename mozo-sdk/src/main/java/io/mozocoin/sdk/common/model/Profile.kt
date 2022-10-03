package io.mozocoin.sdk.common.model

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(indices = [Index(value = ["id", "userId"], unique = true)])
data class Profile(
    @PrimaryKey
    var id: Long = 0L,
    var userId: String? = null,
    @Ignore val avatarUrl: String? = null,
    @Ignore val fullName: String? = null,
    @Ignore val phoneNumber: String? = null,
    @Ignore @SerializedName("birthDay") val birthday: Long? = null,
    @Ignore val email: String? = null,
    @Ignore val gender: String? = null,
    @Embedded var walletInfo: WalletInfo? = null
) : ProfileLocale()