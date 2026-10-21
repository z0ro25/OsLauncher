package com.ezla.oslauncher.themes.data.favorite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// DAO thuần (không suspend) như AppOverrideDao; FavoriteRepository tự đẩy sang thread riêng.
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM themes_favorite WHERE type = :type ORDER BY createdAt DESC")
    fun getByType(type: String): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM themes_favorite WHERE type = :type AND itemId = :itemId)")
    fun exists(type: String, itemId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM themes_favorite WHERE type = :type AND itemId = :itemId")
    fun delete(type: String, itemId: String)
}
