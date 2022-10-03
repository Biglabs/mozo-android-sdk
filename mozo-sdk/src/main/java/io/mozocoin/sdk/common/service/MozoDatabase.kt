package io.mozocoin.sdk.common.service

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.dao.NotificationDao
import io.mozocoin.sdk.common.dao.ProfileDao
import io.mozocoin.sdk.common.dao.UserInfoDao
import io.mozocoin.sdk.common.model.Notification
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database(entities = [UserInfo::class, Profile::class, Notification::class], version = 7, exportSchema = false)
internal abstract class MozoDatabase : RoomDatabase() {

    abstract fun userInfo(): UserInfoDao
    abstract fun profile(): ProfileDao
    abstract fun notifications(): NotificationDao

    fun clear() {
        instance ?: return
        MozoSDK.scope.launch(Dispatchers.IO) {
            if (isOpen) {
                userInfo().deleteAll()
                notifications().deleteAll()
            }
        }
    }

    companion object {
        private var instance: MozoDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TEMPORARY TABLE Notification_backup(id, read, isSend, title, content, type, time, raw)")
                database.execSQL("INSERT INTO Notification_backup SELECT id, read, isSend, title, content, type, time, raw FROM notifications")
                database.execSQL("DROP TABLE notifications")
                database.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `read` INTEGER DEFAULT 0 NOT NULL, `isSend` INTEGER DEFAULT 0 NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `type` TEXT NOT NULL, `time` INTEGER NOT NULL, `raw` TEXT)")
                database.execSQL("INSERT INTO notifications SELECT * FROM Notification_backup")
                database.execSQL("DROP TABLE Notification_backup")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TEMPORARY TABLE Profile_backup(id, userId, status, apiKey, depositAddress, exchangeId, exchangePlatform, exchangeSecret, notificationThreshold, encryptSeedPhrase, offchainAddress)")
                database.execSQL("INSERT INTO Profile_backup SELECT id, userId, status, apiKey, depositAddress, exchangeId, exchangePlatform, exchangeSecret, notificationThreshold, encryptSeedPhrase, offchainAddress FROM Profile")
                database.execSQL("DROP TABLE Profile")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Profile` (`id` INTEGER PRIMARY KEY NOT NULL, `userId` TEXT, `status` TEXT, `apiKey` TEXT, `depositAddress` TEXT, `exchangeId` TEXT, `exchangePlatform` TEXT, `exchangeSecret` TEXT, `notificationThreshold` INTEGER, `encryptSeedPhrase` TEXT, `offchainAddress` TEXT)")
                database.execSQL("CREATE UNIQUE INDEX index_Profile_id_userId ON Profile (id, userId)")
                database.execSQL("INSERT INTO Profile SELECT * FROM Profile_backup")
                database.execSQL("DROP TABLE Profile_backup")
                database.execSQL("ALTER TABLE Profile ADD COLUMN onchainAddress TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Profile ADD COLUMN pin TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TEMPORARY TABLE Profile_backup(id, userId, encryptSeedPhrase, offchainAddress, onchainAddress, pin)")
                database.execSQL("INSERT INTO Profile_backup SELECT id, userId, encryptSeedPhrase, offchainAddress, onchainAddress, pin FROM Profile")
                database.execSQL("DROP TABLE Profile")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Profile` (`id` INTEGER PRIMARY KEY NOT NULL, `userId` TEXT, `encryptSeedPhrase` TEXT, `offchainAddress` TEXT, `onchainAddress` TEXT, `pin` TEXT)")
                database.execSQL("CREATE UNIQUE INDEX index_Profile_id_userId ON Profile (id, userId)")
                database.execSQL("INSERT INTO Profile SELECT * FROM Profile_backup")
                database.execSQL("DROP TABLE Profile_backup")
            }
        }

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance = Room.databaseBuilder(context.applicationContext, MozoDatabase::class.java, "mozo.db")
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
            return@synchronized instance!!
        }

        fun destroyInstance() {
            instance = null
        }
    }
}