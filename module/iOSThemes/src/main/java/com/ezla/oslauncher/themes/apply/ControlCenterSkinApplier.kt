package com.ezla.oslauncher.themes.apply

import android.content.Context
import com.ezla.oslauncher.controlcenter.skin.CcSkin
import com.ezla.oslauncher.controlcenter.skin.CcSkinSlot
import com.ezla.oslauncher.themes.Models.CcSkinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Áp skin Control Center của theme: tải ảnh control_systems về thư mục tạm rồi giao CcSkin cài.
// Ảnh không nhận ra loại ô (name lạ) bị bỏ qua; tải lỗi bất kỳ ảnh nào -> giữ nguyên skin cũ.
object ControlCenterSkinApplier {

    private const val TMP_DIR = "cc_skin_download"

    // [colors] = control_systems.color (1-3 mã hex màu "đang bật"); rỗng = giữ màu gốc của panel.
    suspend fun apply(context: Context, themeId: String, skins: List<CcSkinItem>, colors: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val tmp = File(app.cacheDir, TMP_DIR).apply { deleteRecursively(); mkdirs() }
            try {
                val files = mutableMapOf<CcSkinSlot, File>()
                for (item in skins) {
                    val slot = CcSkinSlot.fromName(item.name) ?: continue
                    if (slot in files) continue
                    val dest = File(tmp, slot.name.lowercase() + ".png")
                    if (!download(item.imageUrl, dest)) return@withContext false
                    files[slot] = dest
                }
                CcSkin.install(app, themeId, files, colors)
            } finally {
                tmp.deleteRecursively()
            }
        }

    // Về nền kính mặc định của Control Center.
    suspend fun restoreDefault(context: Context) = withContext(Dispatchers.IO) {
        CcSkin.clear(context.applicationContext)
    }

    fun appliedThemeId(context: Context): String? = CcSkin.appliedThemeId(context)

    private fun download(url: String, dest: File): Boolean = try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
        conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
        dest.length() > 0L
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    private const val TIMEOUT_MS = 20_000
}
