package com.oslauncher.applauncher.themelauncher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object YourWallpaperDataManager {
    const val ALL_YOUR_WALLPAPER = "ALL_YOUR_WALLPAPER"
    const val IMAGE_SERVER_PATH = "http://51.79.177.162/app/ASA155IOSLauncher/"
    const val IMAGE_SELECTED = "IMAGE_SELECTED"
    const val IMAGE_FROM_GALLERY = "IMAGE_FROM_GALLERY"
    const val LOCK_WALLPAPER_PATH = "LOCK_WALLPAPER_PATH"
    const val IS_EDIT = "IS_EDIT"
    val DefaultData = YourWallpaper(id = 1)

    fun addYourWallpaper(context: Context, data: YourWallpaper) {
        val list = getAllYourWallPaper(context)
        val model = list.lastOrNull { it.id == data.id }
        if (model == null) {
            list.add(data)
        }
        val data = Gson().toJson(list)
        SharePrefUtils.putString(context, ALL_YOUR_WALLPAPER, data)
    }

    fun editYourWallpaper(context: Context, data: YourWallpaper) {
        val list = getAllYourWallPaper(context)
        if (list.lastOrNull { it.id == data.id } != null) {
            val old = list.lastOrNull { it.id == data.id }
            list.add(list.indexOf(old), data)
            list.remove(old)
        }
        val data = Gson().toJson(list)
        SharePrefUtils.putString(context, ALL_YOUR_WALLPAPER, data)
    }

    fun removeYourWallpaper(context: Context, data: YourWallpaper) {
        val list = getAllYourWallPaper(context)
        if (list.contains(data)) {
            list.remove(data)
        }
        val data = Gson().toJson(list)
        SharePrefUtils.putString(context, ALL_YOUR_WALLPAPER, data)
    }

    fun getAllYourWallPaper(context: Context): ArrayList<YourWallpaper> {
        val items: ArrayList<YourWallpaper> = arrayListOf()
        val json = SharePrefUtils.getString(context, ALL_YOUR_WALLPAPER)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<YourWallpaper>>() {}.type
            val allList: List<YourWallpaper> = Gson().fromJson(json, type)
            items.addAll(allList)
        }
        return items
    }

    fun saveYourCurrentWallpaper(context: Context, model: YourWallpaper) {
        val data = Gson().toJson(model)
        SharePrefUtils.putString(context, "Your_Current_Wallpaper", data)
    }


    fun getYourCurrentWallpaper(context: Context): YourWallpaper? {
        val data = SharePrefUtils.getString(context, "Your_Current_Wallpaper")
        if (data.isNullOrEmpty()) return null
        val type = object : TypeToken<YourWallpaper>() {}.type
        return Gson().fromJson(data, type)
    }

    enum class PhotoCategory(val nameString: String, val nameId: Int) {
        WEATHER("Weather", R.string.weather),
        EMOJI("Emoji", R.string.emoji),
        COLOR("Color", R.string.color)
    }

    fun createGradientBitmap(width: Int, height: Int, startColor: Int, endColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            startColor, endColor,
            Shader.TileMode.CLAMP
        )

        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return bitmap
    }

    fun createColorBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = color
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return bitmap
    }

    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        name: String
    ): String {
        val file = File(context.cacheDir, name)
        if (file.exists()) {
            file.delete()
        }

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Save_File_erro", e.message.toString())
            // Handle error
        } finally {
            return if (file.exists()) file.absolutePath else ""
        }
    }

}