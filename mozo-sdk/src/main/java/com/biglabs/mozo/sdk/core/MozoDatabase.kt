package com.biglabs.mozo.sdk.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.dao.AnonymousUserInfoDao
import com.biglabs.mozo.sdk.common.dao.NotificationDao
import com.biglabs.mozo.sdk.common.dao.ProfileDao
import com.biglabs.mozo.sdk.common.dao.UserInfoDao
import com.biglabs.mozo.sdk.common.model.Notification

@Database(entities = [Models.AnonymousUserInfo::class, Models.UserInfo::class, Models.Profile::class, Notification::class], version = 1, exportSchema = false)
internal abstract class MozoDatabase : RoomDatabase() {

    abstract fun anonymousUserInfo(): AnonymousUserInfoDao
    abstract fun userInfo(): UserInfoDao
    abstract fun profile(): ProfileDao
    abstract fun notifications(): NotificationDao

    fun clear() {
        userInfo().deleteAll()
        notifications().deleteAll()
    }

    companion object {
        private var instance: MozoDatabase? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance = Room.databaseBuilder(context.applicationContext, MozoDatabase::class.java, "mozo.db")
                    .fallbackToDestructiveMigration()
                    .build()
            return@synchronized instance!!
        }

        fun destroyInstance() {
            instance = null
        }
    }
}