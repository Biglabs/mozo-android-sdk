package com.biglabs.mozo.sdk.core.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.biglabs.mozo.sdk.core.Models.AnonymousUserInfo

@Dao
interface AnonymousUserInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(userInfo: AnonymousUserInfo)

    @Query("SELECT * FROM AnonymousUserInfo WHERE id = :id")
    fun get(id: Long = 0L): AnonymousUserInfo?
}