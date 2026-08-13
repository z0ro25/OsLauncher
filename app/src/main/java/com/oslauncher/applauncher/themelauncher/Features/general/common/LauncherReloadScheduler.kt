package com.oslauncher.applauncher.themelauncher.Features.general.common

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import com.amz.ios.launcher.LauncherAppState
import com.amz.ios.launcher.compat.UserHandleCompat

/**
 * Gộp (debounce) yêu cầu nạp lại workspace desktop cho các thao tác đổi ICON và đổi TÊN app,
 * ĐỒNG THỜI evict cache in-memory của các component bị đổi trước khi reload.
 *
 * VÌ SAO CẦN DEBOUNCE: mỗi thao tác (đổi icon / đổi tên) ghi IconDB qua broadcast engine rồi cần
 * desktop đọc lại. Nếu MỖI repository tự gọi `LauncherModel.forceReload()` ngay sau broadcast của
 * mình thì khi user làm CẢ HAI liên tiếp, forceReload bị gọi HAI lần sát nhau -> hai lần resetLoadedState
 * chồng lên nhau, đá vào các task ghi IconDB còn xếp hàng trên worker-thread engine -> desktop nạp dở.
 *
 * VÌ SAO CẦN EVICT (bug "đổi cả icon + tên -> tên đổi, icon vẫn cũ"): đã verify trực tiếp source engine
 * + dump IconDB trên máy — IconDB LUÔN ghi ĐÚNG icon custom, nhưng `IconCache.cacheLocked` khi TRÚNG
 * entry in-memory `mCache` thì TRẢ NGAY icon trong cache, CHỈ đọc DB khi entry == null; cuối hàm nó VẪN
 * ghi đè `entry.title` từ prefs (getCustomLabel). Kết quả khi entry cache còn cũ: TÊN mới (đọc prefs) +
 * ICON cũ (giữ trong cache) — đúng triệu chứng user gặp. Khi làm riêng lẻ thì handler icon của engine
 * đã evict cache nên không dính; khi làm cả hai, entry bị nạp lại (còn icon cũ) trước lần reload cuối.
 *
 * CÁCH FIX (app-side, KHÔNG sửa engine): trước khi forceReload, gọi API public `IconCache.remove(component,
 * user)` để xóa entry in-memory của đúng (các) component vừa đổi -> loader buộc đọc lại icon từ IconDB
 * (đã đúng). Vẫn dùng API public, chạy chung process.
 *
 * Debounce chạy trên main-looper (Handler) nên an toàn khi gọi từ Activity/Repository. forceReload tự
 * post công việc nặng sang worker-thread của engine.
 */
object LauncherReloadScheduler {

    /** Cửa sổ gộp: nếu thao tác kế tới trong khoảng này, lịch reload cũ bị dời lại. */
    private const val DEBOUNCE_MS = 250L

    private val handler = Handler(Looper.getMainLooper())

    /** Các component (flatten) vừa đổi icon/tên, sẽ được evict khỏi mCache trước khi reload. */
    private val pendingComponents = LinkedHashSet<String>()

    private val reloadRunnable = Runnable {
        try {
            val iconCache = LauncherAppState.getInstanceNoCreate()?.iconCache
            if (iconCache != null) {
                val user = UserHandleCompat.myUserHandle()
                // Evict entry in-memory của từng component -> loader đọc lại icon từ IconDB (đã đúng).
                for (flatten in pendingComponents) {
                    val cn = ComponentName.unflattenFromString(flatten) ?: continue
                    iconCache.remove(cn, user)
                }
            }
            pendingComponents.clear()
            LauncherAppState.getInstanceNoCreate()?.model?.forceReload()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Hẹn nạp lại workspace desktop cho [componentFlatten] vừa đổi icon/tên. Gọi nhiều lần sát nhau
     * chỉ dẫn tới MỘT lần forceReload (lần cuối), và mọi component đã hẹn đều được evict cache trước
     * khi reload. An toàn gọi từ bất kỳ thread nào — luôn chạy trên main-looper.
     */
    fun scheduleReload(componentFlatten: String? = null) {
        if (!componentFlatten.isNullOrEmpty()) {
            handler.post { pendingComponents.add(componentFlatten) }
        }
        handler.removeCallbacks(reloadRunnable)
        handler.postDelayed(reloadRunnable, DEBOUNCE_MS)
    }
}
