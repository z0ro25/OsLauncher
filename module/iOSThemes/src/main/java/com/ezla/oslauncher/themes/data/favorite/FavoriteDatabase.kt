package com.ezla.oslauncher.themes.data.favorite

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// DB riêng của module Themes, chỉ lưu local. Đổi schema thì tăng version + viết Migration
// (không dùng destructive để khỏi xoá mất danh sách yêu thích của user).
@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteDatabase : RoomDatabase() {

    abstract fun dao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: FavoriteDatabase? = null

        fun get(context: Context): FavoriteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavoriteDatabase::class.java,
                    "themes_favorite.db"
                ).build().also { INSTANCE = it }
            }
    }
}
