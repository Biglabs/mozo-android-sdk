package io.mozocoin.sdk.common.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.mozocoin.sdk.common.Gender

@Entity
data class UserInfo(
        @NonNull @PrimaryKey var id: Long = 0L,
        val userId: String,
        @ColumnInfo(name = "phoneNumber") var phoneNumber: String? = null,
        @ColumnInfo(name = "fullName") var fullName: String? = null,
        @ColumnInfo(name = "avatarUrl") var avatarUrl: String? = null,
        @ColumnInfo(name = "birthday") var birthday: Long = 0L,
        @ColumnInfo(name = "email") var email: String? = null,
        @ColumnInfo(name = "gender") var gender: String? = null
) {
    @Ignore
    fun gender() = Gender.find(gender)
}