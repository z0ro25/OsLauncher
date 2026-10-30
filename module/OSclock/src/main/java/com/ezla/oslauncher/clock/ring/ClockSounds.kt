package com.ezla.oslauncher.clock.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.ezla.oslauncher.clock.R

// Repo không kèm file chuông nên dùng chuông báo thức của máy; "" = chuông báo thức mặc định.
object ClockSounds {

    const val SILENT = "silent"

    data class Sound(val uri: String, val title: String)

    /** Mục đầu là chuông mặc định của máy, sau đó toàn bộ chuông TYPE_ALARM. */
    fun list(context: Context): List<Sound> {
        val out = mutableListOf(Sound("", defaultTitle(context)))
        runCatching {
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_ALARM)
            val c = rm.cursor
            while (c.moveToNext()) {
                val uri = rm.getRingtoneUri(c.position)?.toString() ?: continue
                out += Sound(uri, c.getString(RingtoneManager.TITLE_COLUMN_INDEX))
            }
        }
        return out.distinctBy { it.uri }
    }

    fun title(context: Context, uri: String): String = when (uri) {
        "" -> defaultTitle(context)
        SILENT -> context.getString(R.string.osclock_stop_playing)
        else -> runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
            .getOrNull() ?: defaultTitle(context)
    }

    // Hàng tóm tắt hẹp: chuông mặc định chỉ ghi "Default", tên chuông dài kiểu "Alarm …" bị cắt xấu.
    fun shortTitle(context: Context, uri: String): String =
        if (uri.isEmpty()) context.getString(R.string.osclock_default) else title(context, uri)

    private fun defaultTitle(context: Context): String {
        val name = runCatching {
            RingtoneManager.getRingtone(context, Settings.System.DEFAULT_ALARM_ALERT_URI)?.getTitle(context)
        }.getOrNull()
        return context.getString(R.string.osclock_sound_default, name ?: context.getString(R.string.osclock_sound))
    }

    /** Uri phát được; chuông đã bị xoá khỏi máy thì lùi về mặc định rồi tới chuông thông báo. */
    fun resolve(context: Context, uri: String): Uri? {
        if (uri == SILENT) return null
        if (uri.isNotEmpty()) {
            val parsed = Uri.parse(uri)
            if (runCatching { RingtoneManager.getRingtone(context, parsed) }.getOrNull() != null) return parsed
        }
        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    /** Nghe thử trong màn chọn chuông; nơi gọi giữ lại để stop() khi đổi lựa chọn / đóng màn. */
    fun preview(context: Context, uri: String): Ringtone? {
        val u = resolve(context, uri) ?: return null
        return runCatching {
            RingtoneManager.getRingtone(context, u)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }.getOrNull()
    }
}
