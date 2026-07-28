package com.amz.ios.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amz.ios.launcher.ItemInfo

@Database(entities = [ItemInfo::class], version = 1)
@TypeConverters(Converter::class)
abstract class HiddenDatabase : RoomDatabase() {

    abstract fun databaseDao(): HiddenAppDao

    companion object {
        private var dataBase: HiddenDatabase? = null
        fun createDataBase(context: Context): HiddenDatabase {
            return dataBase ?: synchronized(this) {
                val data = Room.databaseBuilder(
                    context.applicationContext,
                    HiddenDatabase::class.java,
                    "HiddenDatabase"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                dataBase = data
                return dataBase as HiddenDatabase
            }
        }
    }

}