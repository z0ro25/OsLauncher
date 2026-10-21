package com.ezla.oslauncher.themes.base

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

// Lượt áp theme/icon/nền sống sót qua việc màn bị tạo lại: đổi nền -> hệ thống recreate Activity,
// coroutine của lifecycleScope bị huỷ nên mất toast/điều hướng. Kết quả trả về instance ĐANG SỐNG.
class ApplyTask<A : Activity> {

    private var live: WeakReference<A>? = null

    var running = false
        private set

    // Gọi ở initView của mỗi instance (kể cả instance tạo lại) để nhận kết quả.
    fun attach(activity: A) {
        live = WeakReference(activity)
    }

    fun detach(activity: A) {
        if (live?.get() === activity) live = null
    }

    // [work] không được giữ view của màn. [onDone] nhận instance đang sống, null nếu màn đã đóng.
    fun run(work: suspend () -> Boolean, onDone: (A?, Boolean) -> Unit) {
        if (running) return
        running = true
        scope.launch {
            val ok = try {
                work()
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }
            running = false
            onDone(live?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }, ok)
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
