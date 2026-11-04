package com.ezla.oslauncher.controlcenter.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

// App mở được từ launcher (có activity LAUNCHER) để thêm làm ô Control Center.
// Icon/nhãn cache theo package: lưới panel bind lại liên tục, PackageManager chậm.
object CcApps {

    data class Item(val packageName: String, val label: String)

    private val labels = ConcurrentHashMap<String, String>()
    private val icons = ConcurrentHashMap<String, Drawable>()

    // Gọi ở luồng nền: duyệt toàn bộ app có thể mất vài trăm ms. Bỏ chính app này, sắp theo tên.
    fun launchable(c: Context): List<Item> {
        val pm = c.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        return list.asSequence()
            .map { it.activityInfo.packageName }
            .filter { it != c.packageName }
            .distinct()
            .map { pkg -> Item(pkg, label(c, pkg)) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun label(c: Context, pkg: String): String = labels.getOrPut(pkg) {
        runCatching {
            val pm = c.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }

    // null = app đã gỡ: ô vẫn giữ trong danh sách (cài lại là hiện lại) nhưng panel bỏ qua.
    fun icon(c: Context, pkg: String): Drawable? = icons[pkg] ?: runCatching {
        c.packageManager.getApplicationIcon(pkg)
    }.getOrNull()?.also { icons[pkg] = it }

    fun isInstalled(c: Context, pkg: String): Boolean = runCatching {
        c.packageManager.getApplicationInfo(pkg, 0)
        true
    }.getOrDefault(false)

    fun launchIntent(c: Context, pkg: String): Intent? =
        c.packageManager.getLaunchIntentForPackage(pkg)

    // Gọi khi app bị gỡ / cập nhật để lần sau đọc lại icon mới.
    fun invalidate(pkg: String? = null) {
        if (pkg == null) {
            labels.clear()
            icons.clear()
        } else {
            labels.remove(pkg)
            icons.remove(pkg)
        }
    }
}
