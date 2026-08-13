package com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data

import android.content.Context
import com.amz.ios.launcher.appoverride.AppOverrideStore
import com.oslauncher.applauncher.themelauncher.Features.general.common.LauncherReloadScheduler

/**
 * Lưu trữ cho tính năng đổi tên app. NGUỒN SỰ THẬT DUY NHẤT = [AppOverrideStore] (Room app_override.db).
 *
 * Đổi tên CHỈ ghi cột `currentName`; hiển thị luôn đọc `currentName`. Engine đọc cùng Store tại
 * [com.amz.ios.launcher.IconCache.getCustomLabel] nên tên sống trên cả desktop + All Apps, dính sau reboot.
 *
 * Sau khi ghi -> [LauncherReloadScheduler.scheduleReload] (debounce) để launcher nạp lại workspace
 * đọc tên mới. KHÔNG còn broadcast/prefs cũ (đã chuyển hẳn sang Store, hết ghi kép/race).
 *
 * Reset = đưa `currentName` về `defaultName` (nhãn gốc đã seed lúc nạp list).
 */
class RenameAppRepository(context: Context) {

    private val appCtx = context.applicationContext

    init {
        AppOverrideStore.init(appCtx)
    }

    /** Tên current đang áp dụng, hoặc null nếu chưa đặt (khi đó hiển thị = nhãn gốc). */
    fun getCustomLabel(componentFlatten: String): String? =
        AppOverrideStore.getCurrentName(componentFlatten)

    /** Đặt tên mới cho app: ghi current + hẹn reload. Bỏ qua nếu tên rỗng. */
    fun saveName(componentFlatten: String, newName: String) {
        val name = newName.trim()
        if (name.isEmpty()) return
        AppOverrideStore.setCurrentName(componentFlatten, name)
        LauncherReloadScheduler.scheduleReload(componentFlatten)
    }

    /** Đưa tên hiển thị về mặc định (đưa current về default đã seed). */
    fun resetName(componentFlatten: String, defaultLabel: String) {
        AppOverrideStore.resetName(componentFlatten)
        LauncherReloadScheduler.scheduleReload(componentFlatten)
    }
}
