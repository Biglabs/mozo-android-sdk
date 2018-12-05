package com.biglabs.mozo.sdk.common.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.biglabs.mozo.sdk.common.model.Notification

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun save(data: Notification)

    @Query("SELECT * FROM notifications")
    fun getAll(): List<Notification>

    @Query("DELETE from notifications")
    fun deleteAll()
}