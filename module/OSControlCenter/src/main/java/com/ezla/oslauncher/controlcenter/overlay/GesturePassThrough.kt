package com.ezla.oslauncher.controlcenter.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.Looper

// Phát lại chạm / vuốt mà thanh kéo đã nuốt xuống app bên dưới bằng dispatchGesture.
// Bất biến: thanh kéo phải NOT_TOUCHABLE trong lúc phát, nếu không cử chỉ lại rơi vào chính nó.
class GesturePassThrough(
    private val service: AccessibilityService,
    private val setTouchable: (Boolean) -> Unit
) {

    private val main = Handler(Looper.getMainLooper())
    private val restore = Runnable { setTouchable(true) }

    fun replay(points: List<PointF>, durationMs: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || points.isEmpty()) return
        val path = Path().apply {
            moveTo(points.first().x.coerceAtLeast(0f), points.first().y.coerceAtLeast(0f))
            // Chạm tại chỗ: Path cần ít nhất 1 đoạn, dịch 1px để hệ thống không bỏ qua.
            if (points.size == 1) lineTo(points[0].x + 1f, points[0].y)
            points.drop(1).forEach { lineTo(it.x.coerceAtLeast(0f), it.y.coerceAtLeast(0f)) }
        }
        // Giữ đúng thời lượng để app phân biệt chạm / nhấn giữ; chặn trần tránh treo cử chỉ.
        val dur = durationMs.coerceIn(MIN_MS, MAX_MS)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, dur))
            .build()
        main.removeCallbacks(restore)
        setTouchable(false)
        // Chờ WindowManager áp NOT_TOUCHABLE rồi mới phát, nếu không cú chạm vẫn trúng thanh kéo.
        main.postDelayed({
            val ok = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) = done()
                override fun onCancelled(g: GestureDescription?) = done()
            }, main)
            if (!ok) done()
        }, APPLY_DELAY_MS)
        // Phòng hờ callback không về (hệ thống huỷ ngầm): luôn bật chạm lại.
        main.postDelayed(restore, APPLY_DELAY_MS + dur + SAFETY_MS)
    }

    private fun done() {
        main.removeCallbacks(restore)
        main.postDelayed(restore, 50)
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
    }

    private companion object {
        const val MIN_MS = 40L
        const val MAX_MS = 1500L
        const val APPLY_DELAY_MS = 32L
        const val SAFETY_MS = 400L
    }
}
