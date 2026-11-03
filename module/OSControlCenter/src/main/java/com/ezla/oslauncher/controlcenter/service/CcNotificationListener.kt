package com.ezla.oslauncher.controlcenter.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

// Listener riêng của module: nguồn danh sách thông báo (Trung tâm thông báo) + phiên nhạc
// (MediaSessionManager.getActiveSessions cần component listener đang được cấp quyền).
class CcNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        CcNotificationStore.notifyChanged()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        CcNotificationStore.notifyChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = CcNotificationStore.notifyChanged()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = CcNotificationStore.notifyChanged()

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: CcNotificationListener? = null
            private set

        fun active(): List<StatusBarNotification> = runCatching {
            instance?.activeNotifications?.toList()
        }.getOrNull().orEmpty()

        fun dismiss(key: String) {
            runCatching { instance?.cancelNotification(key) }
        }
    }
}
