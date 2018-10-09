package com.biglabs.mozo.sdk.core.dao

import android.arch.persistence.room.*
import com.biglabs.mozo.sdk.core.Models.Profile

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(profiles: List<Profile>)

    @Query("SELECT * FROM Profile WHERE userId = :userId")
    fun get(userId: String): Profile?

    @Query("SELECT * FROM Profile INNER JOIN UserInfo ON UserInfo.userId = Profile.userId WHERE UserInfo.id = 0")
    fun getCurrentUserProfile(): Profile?

    @Query("SELECT * FROM Profile")
    fun getAll(): List<Profile>

    @Delete
    fun delete(profile: Profile)

    @Query("DELETE from Profile")
    fun deleteAll()
}