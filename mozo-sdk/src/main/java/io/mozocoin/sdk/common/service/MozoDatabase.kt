package io.mozocoin.sdk.common.service

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mozocoin.sdk.common.dao.NotificationDao
import io.mozocoin.sdk.common.dao.ProfileDao
import io.mozocoin.sdk.common.dao.UserInfoDao
import io.mozocoin.sdk.common.model.Notification
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.UserInfo

@Database(entities = [UserInfo::class, Profile::class, Notification::class], version = 3, exportSchema = false)
internal abstract class MozoDatabase : RoomDatabase() {

    abstract fun userInfo(): UserInfoDao
    abstract fun profile(): ProfileDao
    abstract fun notifications(): NotificationDao

    fun clear() {
        instance ?: return
        if (isOpen) {
            userInfo().deleteAll()
            notifications().deleteAll()
        }
    }

    companion object {
        private var instance: MozoDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE UserInfo ADD COLUMN avatarUrl TEXT")
                database.execSQL("ALTER TABLE UserInfo ADD COLUMN birthday INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE UserInfo ADD COLUMN email TEXT")
                database.execSQL("ALTER TABLE UserInfo ADD COLUMN gender TEXT")

                database.execSQL("CREATE TEMPORARY TABLE Profile_backup(id, userId, status, apiKey, depositAddress, exchangeId, exchangePlatform, exchangeSecret, notificationThreshold, encryptSeedPhrase, offchainAddress, privateKey)")
                database.execSQL("INSERT INTO Profile_backup SELECT * FROM Profile")
                database.execSQL("DROP TABLE Profile")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Profile` (`id` INTEGER PRIMARY KEY NOT NULL, `userId` TEXT, `status` TEXT, `apiKey` TEXT, `depositAddress` TEXT, `exchangeId` TEXT, `exchangePlatform` TEXT, `exchangeSecret` TEXT, `notificationThreshold` INTEGER, `encryptSeedPhrase` TEXT, `offchainAddress` TEXT, `privateKey` TEXT)")
                database.execSQL("CREATE UNIQUE INDEX index_Profile_id_userId ON Profile (id, userId)")
                database.execSQL("INSERT INTO Profile SELECT * FROM Profile_backup")
                database.execSQL("DROP TABLE Profile_backup")
            }
        }

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance = Room.databaseBuilder(context.applicationContext, MozoDatabase::class.java, "mozo.db")
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
            return@synchronized instance!!
        }

        fun destroyInstance() {
            instance = null
        }
    }
}