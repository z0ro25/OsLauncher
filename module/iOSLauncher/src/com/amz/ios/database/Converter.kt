package com.amz.ios.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.amz.ios.launcher.compat.UserHandleCompat


class Converter {
    @TypeConverter
    fun fromUser(data: UserHandleCompat): String? {
        return Gson().toJson(data)
    }

    @TypeConverter
    fun toUser(string: String): UserHandleCompat? {
        val type = object : TypeToken<UserHandleCompat>() {}.type
        return Gson().fromJson(string, type)
    }

    @TypeConverter
    fun fromChar(char: CharSequence): String? {
        return char.toString()
    }

    @TypeConverter
    fun toChar(string: String): CharSequence? {
        return string
    }

    @TypeConverter
    fun fromIntArray(array: IntArray): String? {
        return Gson().toJson(array)
    }

    @TypeConverter
    fun toIntArray(string: String): IntArray? {
        val type = object : TypeToken<IntArray>() {}.type
        return Gson().fromJson(string, type)
    }
}