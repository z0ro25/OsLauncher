package com.ezla.oslauncher.controlcenter.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.controlcenter.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Quay màn hình (không thu tiếng -> không cần RECORD_AUDIO). Lưu vào Movies/LauncherMiOS.
// Android 14+: phải startForeground(mediaProjection) TRƯỚC khi getMediaProjection, token dùng 1 lần.
class ScreenRecordService : Service() {

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var pfd: ParcelFileDescriptor? = null
    private var outUri: Uri? = null
    private var outFile: File? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() = stopRecording(save = true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording(save = true)
            ACTION_START -> {
                val code = intent.getIntExtra(EXTRA_CODE, 0)
                @Suppress("DEPRECATION")
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (data == null || !startRecording(code, data)) {
                    Toast.makeText(this, R.string.cc_record_failed, Toast.LENGTH_SHORT).show()
                    stopRecording(save = false)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(code: Int, data: Intent): Boolean {
        if (isRecording) return true
        startAsForeground()
        return try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(code, data) ?: return false
            projection = mp
            mp.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
            val dm = resources.displayMetrics
            // Chiều rộng chẵn, tối đa 1080 để encoder H.264 của mọi máy đều nhận.
            val scale = minOf(1f, 1080f / dm.widthPixels)
            val w = ((dm.widthPixels * scale).toInt() / 2) * 2
            val h = ((dm.heightPixels * scale).toInt() / 2) * 2
            val rec = createRecorder()
            rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            prepareOutput(rec)
            rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            rec.setVideoSize(w, h)
            rec.setVideoFrameRate(30)
            rec.setVideoEncodingBitRate(6_000_000)
            rec.prepare()
            recorder = rec
            display = mp.createVirtualDisplay("cc_screen_record", w, h, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, rec.surface, null, null)
            rec.start()
            isRecording = true
            true
        } catch (t: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

    private fun prepareOutput(rec: MediaRecorder) {
        val name = "ScreenRecord_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + FOLDER)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("insert failed")
            outUri = uri
            val fd = contentResolver.openFileDescriptor(uri, "w") ?: throw IllegalStateException("open failed")
            pfd = fd
            rec.setOutputFile(fd.fileDescriptor)
        } else {
            // Android 9-: không có quyền ghi bộ nhớ chung nên lưu thư mục riêng của app.
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), FOLDER).apply { mkdirs() }
            val f = File(dir, name)
            outFile = f
            rec.setOutputFile(f.absolutePath)
        }
    }

    private fun stopRecording(save: Boolean) {
        val wasRecording = isRecording
        isRecording = false
        var ok = false
        runCatching { recorder?.stop(); ok = wasRecording }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { display?.release() }
        display = null
        projection?.let { p ->
            runCatching { p.unregisterCallback(projectionCallback) }
            runCatching { p.stop() }
        }
        projection = null
        runCatching { pfd?.close() }
        pfd = null
        finishOutput(save && ok)
        if (wasRecording) {
            Toast.makeText(this, if (save && ok) R.string.cc_record_saved else R.string.cc_record_failed,
                Toast.LENGTH_SHORT).show()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun finishOutput(keep: Boolean) {
        val uri = outUri
        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                if (keep) {
                    contentResolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                        null, null)
                } else {
                    contentResolver.delete(uri, null, null)
                }
            }
        }
        if (!keep) outFile?.delete()
        outUri = null
        outFile = null
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.cc_record_channel),
                NotificationManager.IMPORTANCE_LOW))
        }
        val stop = PendingIntent.getService(this, 0,
            Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.cc_ic_record)
            .setContentTitle(getString(R.string.cc_recording))
            .setOngoing(true)
            .setContentIntent(stop)
            .addAction(0, getString(R.string.cc_record_stop), stop)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onDestroy() {
        if (isRecording) stopRecording(save = true)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "cc.record.START"
        private const val ACTION_STOP = "cc.record.STOP"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private const val CHANNEL = "cc_screen_record"
        private const val NOTIF_ID = 7102
        private const val FOLDER = "LauncherMiOS"

        @Volatile var isRecording = false
            private set

        fun start(context: Context, code: Int, data: Intent) {
            val i = Intent(context, ScreenRecordService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_CODE, code).putExtra(EXTRA_DATA, data)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, ScreenRecordService::class.java).setAction(ACTION_STOP)) }
        }
    }
}
