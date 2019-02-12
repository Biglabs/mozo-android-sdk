package io.mozocoin.sdk.common.dao

import androidx.room.*
import io.mozocoin.sdk.common.model.Notification

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun save(data: Notification): Long

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun get(id: Long): Notification

    @Query("SELECT * FROM notifications ORDER BY time DESC")
    fun getAll(): List<Notification>

    @Update
    fun updateRead(vararg notifications: Notification)

    @Query("DELETE from notifications")
    fun deleteAll()
}