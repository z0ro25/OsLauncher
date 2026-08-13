package com.amz.ios.launcher.appoverride

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room DB riêng cho tên/logo custom. Version 1 + fallbackToDestructiveMigration:
 * app đang giai đoạn sản xuất đầu tiên, reset sạch dữ liệu OK -> không cần migration.
 */
@Database(entities = [AppOverrideEntity::class], version = 1, exportSchema = false)
abstract class AppOverrideDatabase : RoomDatabase() {

    abstract fun dao(): AppOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: AppOverrideDatabase? = null

        fun get(context: Context): AppOverrideDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppOverrideDatabase::class.java,
                    "app_override.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
