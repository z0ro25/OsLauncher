package com.oslauncher.applauncher.themelauncher.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatDelegate
import com.oslauncher.applauncher.themelauncher.R

/**
 * Quản lý Dark/Light CHỈ cho module app (các màn Kotlin kế thừa BaseActivity = AppCompat).
 *
 * Scope no side-effects:
 *  - KHÔNG đụng launcher desktop (module iOSLauncher là Activity thường) hay module khác.
 *  - Prefs RIÊNG (app_theme_settings), token màu RIÊNG (colors_theme.xml / values-night) ->
 *    không ảnh hưởng màu/màn hiện có.
 *
 * Wallpaper mặc định theo mode:
 *  - Cờ [KEY_USING_DEFAULT_WALLPAPER] = true nghĩa là user ĐANG dùng ảnh mặc định của app.
 *    Khi đó đổi mode sẽ set lại ảnh nền mặc định tương ứng (bg_hello_dark / bg_hello).
 *  - Ngay khi user tự set BẤT KỲ wallpaper nào -> gọi [markUserWallpaper] -> cờ = false ->
 *    đổi mode KHÔNG ghi đè ảnh của user.
 */
object AppThemeManager {

    private const val PREFS = "app_theme_settings"
    private const val KEY_MODE = "app_theme_mode"
    private const val KEY_USING_DEFAULT_WALLPAPER = "app_using_default_wallpaper"

    const val MODE_LIGHT = 0
    const val MODE_DARK = 1

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------
    // Mode
    // ---------------------------------------------------------------------

    fun getMode(context: Context): Int =
        prefs(context).getInt(KEY_MODE, MODE_LIGHT)

    fun isDark(context: Context): Boolean = getMode(context) == MODE_DARK

    /** Áp mode đã lưu cho toàn app. Gọi sớm ở Application.onCreate() (và có thể ở BaseActivity). */
    fun applySavedMode(context: Context) {
        applyDelegate(getMode(context))
    }

    /**
     * Đổi mode: lưu prefs, áp AppCompatDelegate (mọi màn app tự đổi light/dark),
     * và nếu đang dùng ảnh mặc định thì đổi luôn wallpaper mặc định theo mode.
     */
    fun setMode(context: Context, mode: Int) {
        if (mode != MODE_LIGHT && mode != MODE_DARK) return
        prefs(context).edit().putInt(KEY_MODE, mode).apply()
        applyDelegate(mode)
        if (isUsingDefaultWallpaper(context)) {
            applyDefaultWallpaper(context, mode)
        }
    }

    private fun applyDelegate(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            if (mode == MODE_DARK) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // ---------------------------------------------------------------------
    // Wallpaper mặc định vs của user
    // ---------------------------------------------------------------------

    /** Mặc định TRUE: user hiện tại coi như đang dùng ảnh mặc định cho tới khi họ tự đổi. */
    fun isUsingDefaultWallpaper(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USING_DEFAULT_WALLPAPER, true)

    /**
     * Gọi khi user TỰ set wallpaper (bất kỳ ảnh nào) -> từ đây đổi mode không ghi đè ảnh user.
     */
    fun markUserWallpaper(context: Context) {
        prefs(context).edit().putBoolean(KEY_USING_DEFAULT_WALLPAPER, false).apply()
    }

    /**
     * Đánh dấu đang dùng ảnh mặc định của app (dùng khi app chủ động đặt ảnh mặc định).
     */
    fun markDefaultWallpaper(context: Context) {
        prefs(context).edit().putBoolean(KEY_USING_DEFAULT_WALLPAPER, true).apply()
    }

    /**
     * Set ảnh nền mặc định theo mode. Dark dùng drawable `bg_hello_dark` nếu có,
     * fallback về `bg_hello` khi asset dark chưa được thêm -> build/chạy an toàn ngay.
     */
    fun applyDefaultWallpaper(context: Context, mode: Int) {
        val appCtx = context.applicationContext
        val resId = defaultWallpaperRes(appCtx, mode)
        if (resId == 0) return
        Thread {
            try {
                val bm = BitmapFactory.decodeResource(appCtx.resources, resId) ?: return@Thread
                val wm = WallpaperManager.getInstance(appCtx)
                wm.setBitmap(bm)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun defaultWallpaperRes(context: Context, mode: Int): Int {
        if (mode == MODE_DARK) {
            val darkId = context.resources.getIdentifier(
                "bg_hello_dark", "drawable", context.packageName
            )
            if (darkId != 0) return darkId
        }
        return R.drawable.bg_hello
    }
}
