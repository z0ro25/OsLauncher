package com.ezla.oslauncher.themes.apply

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.truongnt.ios.ioslite.common.util.PreferencesUtil
import com.truongnt.ios.launcher.LauncherAppState
import com.truongnt.ios.launcher.appoverride.AppOverrideStore
import com.truongnt.ios.launcher.compat.UserHandleCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Áp theme = tải ảnh (nền + icon) của theme về MỘT folder "theme đang dùng" rồi áp từ file đó:
// nền qua WallpaperManager, icon đè từng app qua AppOverrideStore, cuối cùng reload desktop.
// Đổi theme khác: ghi đè file dùng chung + dọn file theme cũ thừa + reset icon các app chỉ có ở
// theme cũ. CHỈ dùng API public của iOSLauncher — không sửa engine.
object ThemeApplyEngine {

    private const val THEME_DIR = "current_theme"       // folder chứa theme đang dùng
    private const val ICONS_SUBDIR = "icons"
    private const val BG_FILE = "background.png"
    private const val RELOAD_DELAY_MS = 300L

    // Lưu component (flatten) + id của theme đang dùng để lần đổi sau biết reset những app nào.
    private const val KEY_COMPONENTS = "themes_current_components"
    private const val KEY_THEME_ID = "themes_current_theme_id"

    // Áp theme: tải về folder "theme đang dùng" rồi áp. Trả true nếu áp được ít nhất 1 phần.
    suspend fun applyTheme(
        context: Context,
        detail: ThemeDetail,
        applyHome: Boolean,
        applyLock: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        AppOverrideStore.init(appCtx)
        val pm = appCtx.packageManager

        val themeDir = File(appCtx.filesDir, THEME_DIR).apply { if (!exists()) mkdirs() }
        val iconsDir = File(themeDir, ICONS_SUBDIR).apply { if (!exists()) mkdirs() }

        var applied = false

        // 1) Background: tải về folder rồi set nền TỪ FILE (giữ 1 bản background.png của theme).
        val wallpaperUrl = detail.wallpapers.firstOrNull()
        if (wallpaperUrl != null && (applyHome || applyLock)) {
            applied = saveAndSetWallpaper(appCtx, wallpaperUrl, applyHome, applyLock) || applied
        }

        // 2) Icon: tải về folder/icons rồi trỏ AppOverrideStore vào file; gom component theme mới.
        val newComponents = linkedSetOf<String>()
        val newIconFiles = mutableSetOf<String>()
        for (logo in detail.logos) {
            val cn = launchComponent(pm, logo.packageName) ?: continue
            val flatten = cn.flattenToString()
            val name = iconFileName(flatten)
            val file = File(iconsDir, name)
            if (!downloadToFile(logo.imageUrl, file)) continue
            AppOverrideStore.ensureSeeded(flatten, logo.packageName, appLabel(pm, cn))
            AppOverrideStore.setCurrentLogoPath(flatten, file.absolutePath)
            newComponents.add(flatten)
            newIconFiles.add(name)
            applied = true
        }

        // Tải hỏng toàn bộ -> giữ nguyên theme cũ, không reset/không đổi trạng thái.
        if (!applied) return@withContext false

        // 3) Reset icon các app của theme CŨ mà theme MỚI không đổi -> về icon gốc.
        val oldComponents = readComponents(appCtx)
        val toReset = oldComponents - newComponents
        for (flatten in toReset) AppOverrideStore.resetLogo(flatten)

        // 4) Dọn file icon thừa của theme cũ trong folder (giữ folder sạch, chỉ còn theme hiện tại).
        iconsDir.listFiles()?.forEach { if (it.name !in newIconFiles) it.delete() }

        // 5) Lưu trạng thái theme đang dùng.
        writeComponents(appCtx, newComponents)
        PreferencesUtil.putString(appCtx, KEY_THEME_ID, detail.id)

        // 6) Reload desktop: evict cache các app vừa đổi + vừa reset để hiện icon đúng.
        scheduleReload(newComponents + toReset)
        true
    }

    // Đổi riêng NỀN (luồng Wallpaper): cũng thay vào "theme đang dùng" (ghi đè background.png),
    // giữ nguyên phần icon. "Theme" = nền + icon nên đổi lẻ 1 phần vẫn cập nhật folder chung.
    suspend fun applyWallpaper(
        context: Context,
        url: String,
        applyHome: Boolean,
        applyLock: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        saveAndSetWallpaper(context.applicationContext, url, applyHome, applyLock)
    }

    // Tải nền về current_theme/background.png rồi set nền TỪ FILE đó.
    private fun saveAndSetWallpaper(context: Context, url: String, home: Boolean, lock: Boolean): Boolean {
        val dir = File(context.filesDir, THEME_DIR).apply { if (!exists()) mkdirs() }
        val bgFile = File(dir, BG_FILE)
        if (!downloadToFile(url, bgFile)) return false
        return setWallpaperFromFile(context, bgFile, home, lock)
    }

    // Đặt nền từ file đã tải (decode -> setBitmap). N trở lên tách được Home/Lock.
    private fun setWallpaperFromFile(context: Context, file: File, home: Boolean, lock: Boolean): Boolean {
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        return setWallpaper(context, bmp, home, lock)
    }

    private fun setWallpaper(context: Context, bmp: android.graphics.Bitmap, home: Boolean, lock: Boolean): Boolean = try {
        val wm = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var flags = 0
            if (home) flags = flags or WallpaperManager.FLAG_SYSTEM
            if (lock) flags = flags or WallpaperManager.FLAG_LOCK
            wm.setBitmap(bmp, null, true, flags)
        } else {
            wm.setBitmap(bmp)
        }
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    // Tên component khởi chạy của app (khoá override), null nếu app không có launcher intent.
    private fun launchComponent(pm: PackageManager, packageName: String): ComponentName? =
        try {
            pm.getLaunchIntentForPackage(packageName)?.component
        } catch (t: Throwable) {
            null
        }

    private fun appLabel(pm: PackageManager, cn: ComponentName): String = try {
        pm.getActivityInfo(cn, 0).loadLabel(pm).toString()
    } catch (t: Throwable) {
        cn.packageName
    }

    // Tên file ổn định theo component: '/' và ':' -> '_' để không tạo thư mục con.
    private fun iconFileName(flatten: String): String =
        flatten.replace('/', '_').replace(':', '_') + ".png"

    // Tải [url] ghi ra [dest]; ghi file tạm rồi rename để không để lại file dở khi lỗi giữa chừng.
    private fun downloadToFile(url: String, dest: File): Boolean {
        val tmp = File(dest.parentFile, dest.name + ".part")
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (tmp.length() == 0L) {
                tmp.delete()
                false
            } else {
                dest.delete()
                tmp.renameTo(dest)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            tmp.delete()
            false
        }
    }

    // Danh sách component theme đang dùng (phân tách '\n'); rỗng nếu chưa có.
    private fun readComponents(context: Context): Set<String> =
        PreferencesUtil.getString(context, KEY_COMPONENTS, "").orEmpty()
            .split('\n').filter { it.isNotEmpty() }.toSet()

    private fun writeComponents(context: Context, components: Set<String>) {
        PreferencesUtil.putString(context, KEY_COMPONENTS, components.joinToString("\n"))
    }

    // Reload desktop (bản rút gọn của LauncherReloadScheduler ở :app): evict cache in-memory các
    // component đổi/reset rồi forceReload. Trễ 300ms để task ghi AppOverrideStore kịp chốt DB.
    private fun scheduleReload(components: Set<String>) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val app = LauncherAppState.getInstanceNoCreate()
                val iconCache = app?.iconCache
                if (iconCache != null) {
                    val user = UserHandleCompat.myUserHandle()
                    for (flatten in components) {
                        val cn = ComponentName.unflattenFromString(flatten) ?: continue
                        iconCache.remove(cn, user)
                    }
                }
                app?.model?.forceReload()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }, RELOAD_DELAY_MS)
    }
}
