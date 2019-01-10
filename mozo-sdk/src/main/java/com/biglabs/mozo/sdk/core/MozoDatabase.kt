package com.biglabs.mozo.sdk.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.biglabs.mozo.sdk.common.dao.NotificationDao
import com.biglabs.mozo.sdk.common.dao.ProfileDao
import com.biglabs.mozo.sdk.common.dao.UserInfoDao
import com.biglabs.mozo.sdk.common.model.Notification
import com.biglabs.mozo.sdk.common.model.Profile
import com.biglabs.mozo.sdk.common.model.UserInfo

@Database(entities = [UserInfo::class, Profile::class, Notification::class], version = 2, exportSchema = false)
internal abstract class MozoDatabase : RoomDatabase() {

    abstract fun userInfo(): UserInfoDao
    abstract fun profile(): ProfileDao
    abstract fun notifications(): NotificationDao

    fun clear() {
        if (isOpen) {
            userInfo().deleteAll()
            notifications().deleteAll()
        }
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