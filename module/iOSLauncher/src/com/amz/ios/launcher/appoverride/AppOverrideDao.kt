package com.amz.ios.launcher.appoverride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Truy vấn cố định cho bảng app_override (Khuôn A — DAO thuần). */
@Dao
interface AppOverrideDao {

    @Query("SELECT * FROM app_override")
    fun getAll(): List<AppOverrideEntity>

    @Query("SELECT * FROM app_override WHERE componentFlatten = :flatten LIMIT 1")
    fun getByComponent(flatten: String): AppOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: AppOverrideEntity)

    @Query("UPDATE app_override SET currentName = :name WHERE componentFlatten = :flatten")
    fun updateCurrentName(flatten: String, name: String)

    @Query("UPDATE app_override SET currentLogoPath = :path WHERE componentFlatten = :flatten")
    fun updateCurrentLogoPath(flatten: String, path: String?)
}
