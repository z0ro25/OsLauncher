package com.oslauncher.applauncher.themelauncher.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Nhận alarm mốc giờ (06:00 / 19:00) khi policy = AUTO.
 * Chỉ áp lại giao diện đúng theo giờ; [AppThemeManager.applyResolvedMode] tự đặt alarm mốc kế tiếp.
 * Nếu user đã đổi policy khác AUTO thì bỏ qua (alarm cũng đã bị hủy khi đổi policy).
 */
class ThemeAutoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (AppThemeManager.getPolicy(context) == AppThemeManager.POLICY_AUTO) {
            AppThemeManager.applyResolvedMode(context)
        }
    }
}
