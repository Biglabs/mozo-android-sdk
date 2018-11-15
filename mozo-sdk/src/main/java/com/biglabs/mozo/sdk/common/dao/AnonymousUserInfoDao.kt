package com.biglabs.mozo.sdk.common.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.biglabs.mozo.sdk.common.Models.AnonymousUserInfo

@Dao
interface AnonymousUserInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(userInfo: AnonymousUserInfo)

    @Query("SELECT * FROM AnonymousUserInfo WHERE id = :id")
    fun get(id: Long = 0L): AnonymousUserInfo?
}