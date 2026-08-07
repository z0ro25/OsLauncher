package com.oslauncher.applauncher.themelauncher.theme

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatDelegate
import com.oslauncher.applauncher.themelauncher.R
import java.util.Calendar

/**
 * Quản lý Dark/Light CHỈ cho module app (các màn Kotlin kế thừa BaseActivity = AppCompat).
 *
 * Scope no side-effects:
 *  - KHÔNG đụng launcher desktop (module iOSLauncher là Activity thường) hay module khác.
 *  - Prefs RIÊNG (app_theme_settings), token màu RIÊNG (colors_theme.xml / values-night) ->
 *    không ảnh hưởng màu/màn hiện có.
 *
 * Ba policy loại trừ nhau:
 *  - [POLICY_MANUAL]: user tự chọn Light/Dark ([KEY_MODE]).
 *  - [POLICY_AUTO]  : tự đổi theo giờ — Dark 19:00→06:00, Light 06:00→19:00. Đặt AlarmManager
 *    (inexact, không cần quyền) tại 2 mốc để đổi khi qua giờ; mỗi lần mở app cũng re-arm.
 *  - [POLICY_SYSTEM]: theo dark mode hệ thống ([AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM]).
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
    private const val KEY_POLICY = "app_theme_policy"
    private const val KEY_USING_DEFAULT_WALLPAPER = "app_using_default_wallpaper"

    const val MODE_LIGHT = 0
    const val MODE_DARK = 1

    const val POLICY_MANUAL = 0
    const val POLICY_AUTO = 1
    const val POLICY_SYSTEM = 2

    // Mốc giờ cho AUTO: Light [DARK_END_HOUR, DARK_START_HOUR), Dark còn lại.
    private const val DARK_START_HOUR = 19 // 19:00 -> chuyển Dark
    private const val DARK_END_HOUR = 6    // 06:00 -> chuyển Light

    private const val ALARM_REQUEST_CODE = 7301
    private const val ACTION_THEME_AUTO_TICK =
        "com.oslauncher.applauncher.themelauncher.THEME_AUTO_TICK"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------
    // Policy
    // ---------------------------------------------------------------------

    fun getPolicy(context: Context): Int =
        prefs(context).getInt(KEY_POLICY, POLICY_MANUAL)

    private fun setPolicyPref(context: Context, policy: Int) {
        prefs(context).edit().putInt(KEY_POLICY, policy).apply()
    }

    /**
     * Đổi policy (AUTO/SYSTEM) rồi áp lại giao diện. Dùng cho 2 switch màn Appearance.
     * Với MANUAL nên dùng [setManualMode] để kèm luôn light/dark.
     */
    fun setPolicy(context: Context, policy: Int) {
        setPolicyPref(context, policy)
        applyResolvedMode(context)
    }

    // ---------------------------------------------------------------------
    // Mode (light/dark) — chỉ dùng khi policy = MANUAL
    // ---------------------------------------------------------------------

    fun getMode(context: Context): Int =
        prefs(context).getInt(KEY_MODE, MODE_LIGHT)

    /** Dark thực tế đang hiển thị, theo policy hiện hành. */
    fun isDark(context: Context): Boolean = when (getPolicy(context)) {
        POLICY_AUTO -> isDarkByClock()
        POLICY_SYSTEM -> isSystemDark(context)
        else -> getMode(context) == MODE_DARK
    }

    /** Áp mode/policy đã lưu cho toàn app. Gọi sớm ở Application.onCreate(). */
    fun applySavedMode(context: Context) {
        applyResolvedMode(context)
    }

    /**
     * User tự chọn Light/Dark: chuyển về MANUAL, lưu mode, áp lại + hủy alarm AUTO.
     */
    fun setManualMode(context: Context, mode: Int) {
        if (mode != MODE_LIGHT && mode != MODE_DARK) return
        prefs(context).edit()
            .putInt(KEY_MODE, mode)
            .putInt(KEY_POLICY, POLICY_MANUAL)
            .apply()
        applyResolvedMode(context)
    }

    /**
     * Áp giao diện đúng theo policy hiện hành:
     *  - MANUAL: theo [KEY_MODE].
     *  - AUTO  : theo giờ + đặt alarm mốc kế tiếp.
     *  - SYSTEM: theo hệ thống.
     * Đồng thời đổi wallpaper mặc định theo mode nếu đang dùng ảnh default.
     */
    fun applyResolvedMode(context: Context) {
        when (getPolicy(context)) {
            POLICY_AUTO -> {
                val dark = isDarkByClock()
                applyNightMode(if (dark) MODE_DARK else MODE_LIGHT)
                scheduleNextAutoAlarm(context)
                if (isUsingDefaultWallpaper(context)) {
                    applyDefaultWallpaper(context, if (dark) MODE_DARK else MODE_LIGHT)
                }
            }

            POLICY_SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
                cancelAutoAlarm(context)
                if (isUsingDefaultWallpaper(context)) {
                    applyDefaultWallpaper(
                        context,
                        if (isSystemDark(context)) MODE_DARK else MODE_LIGHT
                    )
                }
            }

            else -> {
                val mode = getMode(context)
                applyNightMode(mode)
                cancelAutoAlarm(context)
                if (isUsingDefaultWallpaper(context)) {
                    applyDefaultWallpaper(context, mode)
                }
            }
        }
    }

    private fun applyNightMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            if (mode == MODE_DARK) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // ---------------------------------------------------------------------
    // AUTO — tính theo giờ + AlarmManager (inexact, không cần quyền)
    // ---------------------------------------------------------------------

    /** Dark khi giờ hiện tại ngoài khoảng [DARK_END_HOUR, DARK_START_HOUR). */
    private fun isDarkByClock(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= DARK_START_HOUR || hour < DARK_END_HOUR
    }

    private fun isSystemDark(context: Context): Boolean {
        val night = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return night == Configuration.UI_MODE_NIGHT_YES
    }

    private fun autoAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, ThemeAutoReceiver::class.java).apply {
            action = ACTION_THEME_AUTO_TICK
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context.applicationContext, ALARM_REQUEST_CODE, intent, flags
        )
    }

    /** Đặt alarm inexact tới mốc chuyển (06:00 hoặc 19:00) gần nhất trong tương lai. */
    fun scheduleNextAutoAlarm(context: Context) {
        val am = context.applicationContext
            .getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.set(AlarmManager.RTC, nextSwitchTimeMillis(), autoAlarmIntent(context))
    }

    fun cancelAutoAlarm(context: Context) {
        val am = context.applicationContext
            .getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(autoAlarmIntent(context))
    }

    /** Thời điểm mốc 06:00 hoặc 19:00 kế tiếp (mili giây). */
    private fun nextSwitchTimeMillis(): Long {
        val now = Calendar.getInstance()

        val morning = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, DARK_END_HOUR)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val evening = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, DARK_START_HOUR)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        // Mốc nào đã qua thì đẩy sang ngày mai.
        if (morning.timeInMillis <= now.timeInMillis) morning.add(Calendar.DAY_OF_YEAR, 1)
        if (evening.timeInMillis <= now.timeInMillis) evening.add(Calendar.DAY_OF_YEAR, 1)

        return minOf(morning.timeInMillis, evening.timeInMillis)
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
