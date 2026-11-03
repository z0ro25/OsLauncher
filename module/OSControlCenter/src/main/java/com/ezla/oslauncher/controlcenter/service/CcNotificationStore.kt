package com.ezla.oslauncher.controlcenter.service

import android.os.Handler
import android.os.Looper

// Phát tín hiệu "danh sách thông báo đổi" từ listener sang overlay (cùng process).
object CcNotificationStore {

    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<() -> Unit>()

    fun addListener(l: () -> Unit) {
        listeners += l
    }

    fun removeListener(l: () -> Unit) {
        listeners -= l
    }

    fun notifyChanged() {
        main.post { listeners.toList().forEach { it() } }
    }
}
