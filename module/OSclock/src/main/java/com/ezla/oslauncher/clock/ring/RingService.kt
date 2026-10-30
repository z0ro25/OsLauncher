package com.ezla.oslauncher.clock.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.ClockStore

// Foreground service phát chuông + rung cho 1 báo thức / hẹn giờ đang kêu, kèm notification
// full-screen mở RingActivity. Có chuông mới thì chuông cũ bị thay (giống iOS chỉ kêu 1 cái).
class RingService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { if (kind == KIND_ALARM) snooze(this) else dismiss(this) }
    private var kind = KIND_ALARM
    private var id = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        kind = intent?.getIntExtra(EXTRA_KIND, KIND_ALARM) ?: KIND_ALARM
        id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        // Mở bằng startForegroundService thì luôn phải gọi startForeground, kể cả khi bỏ chuông
        // (alarm vừa bị xoá) — không thì app crash ForegroundServiceDidNotStart.
        val loaded = RingInfo.load(this, kind, id)
        val info = loaded ?: RingInfo(kind, id, "", "", false, false, 0, 0, 0)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(this, info),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
        if (loaded == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        current = info
        acquireWakeLock()
        startSound(info)
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
        // Màn hình đang mở thì Android không bật full-screen intent -> tự mở màn chuông.
        runCatching { startActivity(RingActivity.intent(this, info).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        stopSound()
        wakeLock?.takeIf { it.isHeld }?.release()
        current = null
        RingActivity.finishAll()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osclock:ring").apply {
            setReferenceCounted(false)
            acquire(AUTO_STOP_MS + 5_000)
        }
    }

    private fun startSound(info: RingInfo) {
        stopSound()
        ClockSounds.resolve(this, info.sound)?.let { uri ->
            player = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    )
                    setDataSource(this@RingService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.onFailure { Log.w(TAG, "Không phát được chuông $uri", it) }.getOrNull()
        }
        if (info.vibrate) vibrator().let { v ->
            val pattern = longArrayOf(0, 600, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else @Suppress("DEPRECATION") v.vibrate(pattern, 0)
        }
    }

    private fun stopSound() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator().cancel()
    }

    @Suppress("DEPRECATION")
    private fun vibrator() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    companion object {
        private const val TAG = "OSclock"
        const val KIND_ALARM = 0
        const val KIND_TIMER = 1
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val CHANNEL_RING = "osclock_ring"
        private const val NOTIFICATION_ID = 7101
        const val SNOOZE_MS = 9 * 60_000L
        // iOS kêu ~15 phút; ở đây 10 phút rồi tự snooze (alarm) / tự tắt (timer).
        private const val AUTO_STOP_MS = 10 * 60_000L

        /** Chuông đang kêu (null = không có) — RingActivity đọc để hiển thị. */
        @Volatile var current: RingInfo? = null
            private set

        fun fire(context: Context, kind: Int, id: Int) {
            val info = RingInfo.load(context, kind, id) ?: return
            if (kind == KIND_ALARM) onAlarmFired(context, id) else onTimerFired(context, id)
            val i = Intent(context, RingService::class.java).putExtra(EXTRA_KIND, kind).putExtra(EXTRA_ID, id)
            // Android 12+ có thể chặn mở FGS từ nền (lịch không exact): vẫn hiện notification
            // full-screen, RingActivity mở lên sẽ tự khởi service khi đã ở foreground.
            runCatching { ContextCompat.startForegroundService(context, i) }.onFailure {
                Log.w(TAG, "Không mở được RingService từ nền", it)
                ensureChannel(context)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, buildNotification(context, info, startService = true))
            }
        }

        // Một lần: tắt sau khi kêu. Lặp: hẹn ngay lần kế tiếp. Xoá mốc snooze cũ.
        private fun onAlarmFired(context: Context, id: Int) {
            val store = ClockStore(context)
            val a = store.alarm(id) ?: return
            val next = if (a.days and ClockMath.EVERY_DAY == 0) a.copy(enabled = false, snoozeUntil = 0)
            else a.copy(snoozeUntil = 0)
            store.saveAlarm(next)
            AlarmScheduler.schedule(context, next)
        }

        private fun onTimerFired(context: Context, id: Int) {
            val store = ClockStore(context)
            store.timer(id)?.let { store.saveTimer(it.copy(endAt = 0L, remainingMs = 0L)) }
            TimerNotifier.refresh(context)
        }

        fun snooze(context: Context) {
            val info = current
            if (info != null && info.kind == KIND_ALARM && info.snooze) {
                val store = ClockStore(context)
                store.alarm(info.id)?.let { a ->
                    val s = a.copy(enabled = true, snoozeUntil = System.currentTimeMillis() + SNOOZE_MS)
                    store.saveAlarm(s)
                    AlarmScheduler.schedule(context, s)
                    SnoozeNotifier.show(context, s)
                }
            }
            stop(context)
        }

        /** Timer: Repeat = chạy lại đúng thời lượng cũ. */
        fun repeatTimer(context: Context) {
            val info = current
            if (info != null && info.kind == KIND_TIMER) {
                val store = ClockStore(context)
                store.timer(info.id)?.let { t ->
                    val end = System.currentTimeMillis() + t.durationMs
                    store.saveTimer(t.copy(endAt = end, remainingMs = t.durationMs))
                    AlarmScheduler.scheduleTimer(context, t.id, end)
                    TimerNotifier.refresh(context)
                }
            }
            stop(context)
        }

        fun dismiss(context: Context) {
            val info = current
            if (info?.kind == KIND_TIMER) {
                ClockStore(context).deleteTimer(info.id)
                TimerNotifier.refresh(context)
            }
            stop(context)
        }

        fun cancelSnooze(context: Context, id: Int) {
            val store = ClockStore(context)
            store.alarm(id)?.let { a ->
                val c = a.copy(snoozeUntil = 0, enabled = a.days and ClockMath.EVERY_DAY != 0)
                store.saveAlarm(c)
                AlarmScheduler.schedule(context, c)
            }
            SnoozeNotifier.cancel(context, id)
        }

        private fun stop(context: Context) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            runCatching { context.stopService(Intent(context, RingService::class.java)) }
            current = null
            RingActivity.finishAll()
        }

        fun startFromActivity(context: Context, info: RingInfo) {
            if (current != null) return
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, RingService::class.java).putExtra(EXTRA_KIND, info.kind).putExtra(EXTRA_ID, info.id)
                )
            }
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_RING) != null) return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_RING, context.getString(R.string.osclock_channel_ring), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null) // service tự phát chuông
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }

        private fun buildNotification(context: Context, info: RingInfo, startService: Boolean = false): Notification {
            ensureChannel(context)
            val full = PendingIntent.getActivity(
                context, NOTIFICATION_ID, RingActivity.intent(context, info, startService),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val b = NotificationCompat.Builder(context, CHANNEL_RING)
                .setSmallIcon(R.drawable.osclock_ic_tab_alarm)
                .setContentTitle(info.title(context))
                .setContentText(info.subtitle(context))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
            if (info.kind == KIND_ALARM && info.snooze) {
                b.addAction(0, context.getString(R.string.osclock_snooze), receiverPending(context, AlarmReceiver.ACTION_SNOOZE))
            }
            b.addAction(0, context.getString(R.string.osclock_stop), receiverPending(context, AlarmReceiver.ACTION_DISMISS))
            return b.build()
        }

        private fun receiverPending(context: Context, action: String) = PendingIntent.getBroadcast(
            context, action.hashCode(), Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )    }
}
