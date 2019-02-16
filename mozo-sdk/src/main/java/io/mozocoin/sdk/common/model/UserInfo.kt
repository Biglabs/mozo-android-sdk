package io.mozocoin.sdk.common.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserInfo(
        @NonNull @PrimaryKey var id: Long = 0L,
        val userId: String,
        @ColumnInfo(name = "phoneNumber") val phoneNumber: String? = null,
        @ColumnInfo(name = "fullName") val fullName: String? = null,
        @ColumnInfo(name = "avatarUrl") val avatarUrl: String? = null,
        @ColumnInfo(name = "birthday") val birthday: Long = 0L,
        @ColumnInfo(name = "email") val email: String? = null,
        @ColumnInfo(name = "gender") val gender: String? = null
)