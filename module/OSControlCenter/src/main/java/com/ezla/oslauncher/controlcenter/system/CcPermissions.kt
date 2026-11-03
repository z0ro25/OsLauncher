package com.ezla.oslauncher.controlcenter.system

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import com.ezla.oslauncher.controlcenter.service.CcAccessibilityService
import com.ezla.oslauncher.controlcenter.service.CcNotificationListener

object CcPermissions {

    // Đọc từ Settings.Secure thay vì instance tĩnh: service có thể chưa kịp bind khi vừa bật.
    fun isAccessibilityEnabled(c: Context): Boolean {
        val enabled = Settings.Secure.getString(
            c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(c, CcAccessibilityService::class.java)
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { ComponentName.unflattenFromString(it) == me }
    }

    fun isNotificationListenerEnabled(c: Context): Boolean {
        val flat = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners")
            ?: return false
        val me = ComponentName(c, CcNotificationListener::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun isIgnoringBatteryOptimizations(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(c.packageName)
    }

    fun canWriteSettings(c: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(c)

    fun hasNotificationPolicy(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun allGranted(c: Context) = isAccessibilityEnabled(c) &&
        isNotificationListenerEnabled(c) && isIgnoringBatteryOptimizations(c)

    fun accessibilitySettings() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun notificationListenerSettings() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    // Dùng màn danh sách thay ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: intent đó cần quyền
    // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS mà Google Play hạn chế.
    fun batterySettings() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun writeSettings(c: Context) = Intent(
        Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.packageName)
    )

    fun notificationPolicySettings() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
