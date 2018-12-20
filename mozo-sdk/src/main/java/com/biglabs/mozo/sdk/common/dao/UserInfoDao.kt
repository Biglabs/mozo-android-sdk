package com.biglabs.mozo.sdk.common.dao

import androidx.room.*
import com.biglabs.mozo.sdk.common.Models.UserInfo

@Dao
interface UserInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(userInfo: UserInfo)

    @Query("SELECT * FROM UserInfo WHERE id = :id")
    fun get(id: Long = 0L): UserInfo?

    @Query("SELECT * FROM UserInfo")
    fun getAll(): List<UserInfo>

    @Query("DELETE from UserInfo")
    fun deleteAll()
}