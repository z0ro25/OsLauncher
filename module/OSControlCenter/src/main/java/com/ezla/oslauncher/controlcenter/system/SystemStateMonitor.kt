package com.ezla.oslauncher.controlcenter.system

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat

// Gom trạng thái hệ thống panel cần hiển thị; báo [onChanged] mỗi khi có thay đổi.
// start()/stop() đối xứng, gọi lại start() sau stop() vẫn đăng ký lại đủ (lỗi cũ của Theme4k).
class SystemStateMonitor(context: Context, private val onChanged: () -> Unit) {

    private val app = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var running = false

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = onChanged()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = onChanged()
    }

    fun start() {
        if (running) return
        running = true
        val cr = app.contentResolver
        cr.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, observer)
        cr.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, observer)
        cr.registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, observer)
        // Âm lượng: không có URI riêng ổn định giữa các hãng nên nghe cả Settings.System.
        cr.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun stop() {
        if (!running) return
        running = false
        app.contentResolver.unregisterContentObserver(observer)
        runCatching { app.unregisterReceiver(receiver) }
    }

    // ===== Đọc trạng thái =====

    fun brightness(): Int = runCatching {
        Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(128)

    fun setBrightness(value: Int): Boolean {
        if (!CcPermissions.canWriteSettings(app)) return false
        return runCatching {
            // Đang tự động thì kéo tay không có tác dụng -> chuyển sang chỉnh tay như iOS.
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value.coerceIn(1, 255))
        }.getOrDefault(false)
    }

    fun volumeFraction(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun setVolumeFraction(f: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(f * max), 0) }
    }

    fun isRotationLocked(): Boolean = runCatching {
        Settings.System.getInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0
    }.getOrDefault(false)

    fun toggleRotationLock(): Boolean {
        if (!CcPermissions.canWriteSettings(app)) return false
        return runCatching {
            Settings.System.putInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION,
                if (isRotationLocked()) 1 else 0)
        }.getOrDefault(false)
    }

    // Ô chuông gạch = chế độ im lặng chuông (ringer). Focus mở màn DND hệ thống nên tách riêng.
    fun isSilent() = audio.ringerMode != AudioManager.RINGER_MODE_NORMAL

    fun toggleSilent(): Boolean {
        val target = if (isSilent()) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_VIBRATE
        return runCatching { audio.ringerMode = target; true }.getOrDefault(false)
    }

    fun isDndOn(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    fun isAirplaneOn() =
        Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    fun isWifiOn(): Boolean = runCatching {
        (app.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    fun isBluetoothOn(): Boolean = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }
        .getOrDefault(false)

    // Không có quyền READ_PHONE_STATE nên suy ra từ mạng đang dùng có phải di động không.
    fun isCellularOn(): Boolean = runCatching {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true }
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }.getOrDefault(false)

    fun isSyncOn(): Boolean = runCatching { android.content.ContentResolver.getMasterSyncAutomatically() }
        .getOrDefault(false)

    fun isPowerSave(): Boolean = runCatching {
        (app.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isPowerSaveMode
    }.getOrDefault(false)

    fun isDarkMode(): Boolean =
        (app.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
}
