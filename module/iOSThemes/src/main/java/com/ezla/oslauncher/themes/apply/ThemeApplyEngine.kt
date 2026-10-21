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
        var applied = false

        // 1) Background: tải về folder rồi set nền TỪ FILE (giữ 1 bản background.png của theme).
        val wallpaperUrl = detail.wallpapers.firstOrNull()
        if (wallpaperUrl != null && (applyHome || applyLock)) {
            applied = saveAndSetWallpaper(appCtx, wallpaperUrl, applyHome, applyLock)
        }

        // 2) Icon: thay phần icon của "theme đang dùng" (giữ nền).
        replaceIcons(appCtx, detail) || applied
    }

    // Đổi riêng ICON (tab Icon ở màn Install): thay phần icon của "theme đang dùng", giữ nền.
    // Về bộ icon mặc định: reset logo mọi app đã đổi theo theme, xoá icon trong folder, reload
    // desktop. Giữ nguyên nền. Trả false nếu đang không áp bộ icon nào.
    suspend fun restoreDefaultIcons(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        AppOverrideStore.init(appCtx)
        val old = readComponents(appCtx)
        if (old.isEmpty()) return@withContext false
        for (flatten in old) AppOverrideStore.resetLogo(flatten)
        File(File(appCtx.filesDir, THEME_DIR), ICONS_SUBDIR).listFiles()?.forEach { it.delete() }
        writeComponents(appCtx, emptySet())
        PreferencesUtil.putString(appCtx, KEY_THEME_ID, "")
        scheduleReload(old)
        true
    }

    // Về nền mặc định của launcher (ảnh drawable [wallpaperRes]) cho Home/Lock; xoá background.png
    // của "theme đang dùng" vì nền giờ không còn thuộc theme nào.
    suspend fun restoreDefaultWallpaper(
        context: Context,
        wallpaperRes: Int,
        applyHome: Boolean,
        applyLock: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val bmp = BitmapFactory.decodeResource(appCtx.resources, wallpaperRes) ?: return@withContext false
        val ok = setWallpaper(appCtx, bmp, applyHome, applyLock)
        if (ok) File(File(appCtx.filesDir, THEME_DIR), BG_FILE).delete()
        ok
    }

    suspend fun applyIcons(context: Context, detail: ThemeDetail): Boolean =
        withContext(Dispatchers.IO) { replaceIcons(context.applicationContext, detail) }

    // Tải icon về current_theme/icons, trỏ AppOverrideStore vào file, reset icon app chỉ có ở bộ
    // cũ, dọn file thừa rồi reload desktop. Tải hỏng toàn bộ -> giữ nguyên bộ icon cũ, trả false.
    private fun replaceIcons(appCtx: Context, detail: ThemeDetail): Boolean {
        AppOverrideStore.init(appCtx)
        val pm = appCtx.packageManager
        val themeDir = File(appCtx.filesDir, THEME_DIR).apply { if (!exists()) mkdirs() }
        val iconsDir = File(themeDir, ICONS_SUBDIR).apply { if (!exists()) mkdirs() }
        var applied = false

        // Tải từng logo về folder/icons rồi trỏ AppOverrideStore vào file; gom component bộ mới.
        val newComponents = linkedSetOf<String>()
        val newIconFiles = mutableSetOf<String>()
        for (logo in detail.logos) {
            val cn = AppCodeResolver.resolve(appCtx, logo.packageName) ?: continue
            val flatten = cn.flattenToString()
            val name = iconFileName(flatten)
            val file = File(iconsDir, name)
            if (!downloadToFile(logo.imageUrl, file)) continue
            // Cắt lề trong suốt/bóng thừa để thân icon to gần bằng app khác trên desktop.
            IconTrimmer.trim(file)
            AppOverrideStore.ensureSeeded(flatten, logo.packageName, appLabel(pm, cn))
            AppOverrideStore.setCurrentLogoPath(flatten, file.absolutePath)
            newComponents.add(flatten)
            newIconFiles.add(name)
            applied = true
        }

        // Tải hỏng toàn bộ -> giữ nguyên bộ icon cũ, không reset/không đổi trạng thái.
        if (!applied) return false

        // Reset icon các app của bộ CŨ mà bộ MỚI không đổi -> về icon gốc.
        val oldComponents = readComponents(appCtx)
        val toReset = oldComponents - newComponents
        for (flatten in toReset) AppOverrideStore.resetLogo(flatten)

        // Dọn file icon thừa của bộ cũ (folder chỉ còn bộ hiện tại).
        iconsDir.listFiles()?.forEach { if (it.name !in newIconFiles) it.delete() }

        writeComponents(appCtx, newComponents)
        PreferencesUtil.putString(appCtx, KEY_THEME_ID, detail.id)

        // Reload desktop: evict cache các app vừa đổi + vừa reset để hiện icon đúng.
        scheduleReload(newComponents + toReset)
        return true
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
