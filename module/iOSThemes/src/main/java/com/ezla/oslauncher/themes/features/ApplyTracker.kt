package com.ezla.oslauncher.themes.features

import com.truongnt.ios.ioslite.common.analytics.AnalyticsDelegate

// Event Firebase "user_apply": chỉ gọi khi áp dụng THÀNH CÔNG (huỷ / thiếu xu / lỗi không tính).
object ApplyTracker {
    const val TYPE_ICONS = "icons"
    const val TYPE_BACKGROUND = "background"
    const val TYPE_BOTH = "both"

    fun log(theme: String, type: String) {
        AnalyticsDelegate.logEvent("user_apply", mapOf("theme" to theme, "type" to type))
    }
}
