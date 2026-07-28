package com.amz.ios.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.amz.ios.launcher.ItemInfo

@Dao
interface HiddenAppDao {

    @Query("SELECT * FROM iteminfo")
    fun getAllHiddenApp() : List<ItemInfo>

    @Insert
    fun hideApp(data : ItemInfo)

    @Delete
    fun unHideApp(data: ItemInfo)
}