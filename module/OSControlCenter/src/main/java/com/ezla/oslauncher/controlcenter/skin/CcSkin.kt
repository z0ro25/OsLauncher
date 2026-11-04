package com.ezla.oslauncher.controlcenter.skin

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.ezla.oslauncher.controlcenter.CcPrefs
import java.io.File

// Bộ ảnh nền ô panel của theme đang áp (module Themes tải về rồi gọi install). File nằm ở
// filesDir/cc_skin/<slot>.png; chưa áp / đã gỡ thì panel dùng nền kính mặc định.
object CcSkin {

    private const val DIR = "cc_skin"
    private const val KEY_THEME_ID = "skin_theme_id"

    @Volatile private var cache: Map<CcSkinSlot, Drawable>? = null

    private fun dir(c: Context) = File(c.applicationContext.filesDir, DIR)

    private fun file(c: Context, slot: CcSkinSlot) = File(dir(c), slot.name.lowercase() + ".png")

    // Id theme đang áp làm skin; null = mặc định.
    fun appliedThemeId(c: Context): String? = CcPrefs.prefs(c).getString(KEY_THEME_ID, null)

    // Chép ảnh vào thư mục skin (xoá bộ cũ trước để không lẫn ô của theme trước). Ghi key cuối
    // cùng: listener prefs ở panel đọc lại skin khi key đổi.
    fun install(c: Context, themeId: String, files: Map<CcSkinSlot, File>): Boolean {
        if (files.isEmpty()) return false
        val target = dir(c)
        target.deleteRecursively()
        if (!target.mkdirs()) return false
        files.forEach { (slot, src) -> src.copyTo(file(c, slot), overwrite = true) }
        cache = null
        CcPrefs.prefs(c).edit().putString(KEY_THEME_ID, themeId).apply()
        return true
    }

    // Bản đầy đủ: ảnh nền ô + màu "đang bật" (1-3 mã hex). Theme chỉ có màu / chỉ có ảnh vẫn áp được;
    // không có cả hai -> false, giữ nguyên skin cũ. Màu ghi trước, key theme ghi cuối (panel đọc lại).
    fun install(c: Context, themeId: String, files: Map<CcSkinSlot, File>, accentHex: List<String>?): Boolean {
        val accent = CcAccent.parse(accentHex)
        if (files.isEmpty() && accent == null) return false
        val target = dir(c)
        target.deleteRecursively()
        if (files.isNotEmpty() && !target.mkdirs()) return false
        files.forEach { (slot, src) -> src.copyTo(file(c, slot), overwrite = true) }
        cache = null
        CcAccent.install(c, accent)
        CcPrefs.prefs(c).edit().putString(KEY_THEME_ID, themeId).apply()
        return true
    }

    fun clear(c: Context) {
        dir(c).deleteRecursively()
        cache = null
        CcAccent.install(c, null)
        CcPrefs.prefs(c).edit().remove(KEY_THEME_ID).apply()
    }

    // Đọc lười, giữ trong bộ nhớ tới lần install/clear kế tiếp.
    fun drawables(c: Context): Map<CcSkinSlot, Drawable> {
        cache?.let { return it }
        val res = c.resources
        val loaded = CcSkinSlot.values().mapNotNull { slot ->
            val f = file(c, slot)
            if (!f.exists()) return@mapNotNull null
            BitmapFactory.decodeFile(f.absolutePath)?.let { slot to BitmapDrawable(res, it) }
        }.toMap()
        cache = loaded
        return loaded
    }

    // Mỗi view cần drawable riêng (cùng drawable gắn nhiều view sẽ dùng chung bounds).
    fun drawable(c: Context, slot: CcSkinSlot): Drawable? =
        drawables(c)[slot]?.constantState?.newDrawable(c.resources)?.mutate()
}
