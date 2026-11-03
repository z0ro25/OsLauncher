package com.ezla.oslauncher.controlcenter.system

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import com.ezla.oslauncher.controlcenter.ControlCenterEntry
import com.ezla.oslauncher.controlcenter.R

// Mở màn hệ thống / app từ overlay. Context là service nên luôn kèm NEW_TASK.
object CcActions {

    fun start(c: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            c.startActivity(intent)
            true
        } catch (t: Throwable) {
            false
        }
    }

    // Thử lần lượt từng intent, cái nào mở được thì dừng; hết thì báo không có app.
    fun startFirst(c: Context, vararg intents: Intent) {
        if (intents.none { start(c, it) }) {
            Toast.makeText(c, R.string.cc_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    fun airplane(c: Context) = startFirst(c, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS))

    fun wifi(c: Context) {
        val list = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) list += Intent(Settings.Panel.ACTION_WIFI)
        list += Intent(Settings.ACTION_WIFI_SETTINGS)
        startFirst(c, *list.toTypedArray())
    }

    fun cellular(c: Context) {
        val list = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list += Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        }
        list += Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        list += Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startFirst(c, *list.toTypedArray())
    }

    fun bluetooth(c: Context) = startFirst(c, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

    // Hotspot không có action public: thử màn tethering của AOSP rồi lùi về Wireless.
    fun hotspot(c: Context) = startFirst(
        c,
        Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
        Intent("android.settings.TETHER_SETTINGS"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS)
    )

    fun sync(c: Context) = startFirst(c, Intent(Settings.ACTION_SYNC_SETTINGS), Intent(Settings.ACTION_SETTINGS))

    fun focus(c: Context) = startFirst(c, Intent("android.settings.ZEN_MODE_SETTINGS"),
        Intent(Settings.ACTION_SOUND_SETTINGS))

    fun darkMode(c: Context) = startFirst(c, Intent(Settings.ACTION_DISPLAY_SETTINGS))

    fun lowPower(c: Context) = startFirst(c, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY))

    fun settings(c: Context) = startFirst(c, Intent(Settings.ACTION_SETTINGS))

    fun camera(c: Context) = startFirst(c, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent(MediaStore.ACTION_IMAGE_CAPTURE))

    // Ưu tiên app Clock / Calculator của launcher (:app gắn hook), không có thì dùng app hệ thống.
    fun alarm(c: Context) {
        val opener = ControlCenterEntry.alarmOpener
        if (opener != null) opener(c) else startFirst(c, Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    fun calculator(c: Context) {
        val opener = ControlCenterEntry.calculatorOpener
        if (opener != null) {
            opener(c)
            return
        }
        val byCategory = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        val pkgs = listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator",
            "com.miui.calculator", "com.coloros.calculator", "com.android.calculator2")
        startFirst(c, byCategory, *launchIntents(c, pkgs))
    }

    fun voiceMemo(c: Context) {
        val pkgs = listOf("com.google.android.apps.recorder", "com.sec.android.app.voicenote",
            "com.android.soundrecorder", "com.coloros.soundrecorder", "com.miui.soundrecorder")
        startFirst(c, Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION), *launchIntents(c, pkgs))
    }

    private fun launchIntents(c: Context, pkgs: List<String>): Array<Intent> {
        val pm: PackageManager = c.packageManager
        return pkgs.mapNotNull { runCatching { pm.getLaunchIntentForPackage(it) }.getOrNull() }
            .toTypedArray()
    }

    fun appDetails(c: Context, pkg: String) = start(
        c, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
    )

    // Android 14+ chặn PendingIntent của app khác mở Activity từ nền nếu bên gửi không cho phép.
    fun sendPendingIntent(pi: PendingIntent): Boolean = try {
        if (Build.VERSION.SDK_INT >= 34) {
            val opts = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            pi.send(null, 0, null, null, null, null, opts.toBundle())
        } else {
            pi.send()
        }
        true
    } catch (t: Throwable) {
        false
    }
}
