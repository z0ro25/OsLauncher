package com.ezla.oslauncher.controlcenter.system

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.ezla.oslauncher.controlcenter.service.CcNotificationListener

// Phiên nhạc đang phát (lấy qua MediaSessionManager, cần quyền Notification access).
class MediaMonitor(context: Context, private val onChanged: (Info?) -> Unit) {

    data class Info(
        val title: String?,
        val artist: String?,
        val art: Bitmap?,
        val playing: Boolean,
        val position: Long,
        val duration: Long,
        val hasNext: Boolean,
        val hasPrev: Boolean,
        val appPackage: String
    )

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val msm = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(app, CcNotificationListener::class.java)
    private var controller: MediaController? = null
    private var running = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        bind(list?.firstOrNull())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = bind(null)
    }

    fun start() {
        if (running) return
        if (!CcPermissions.isNotificationListenerEnabled(app)) {
            onChanged(null)
            return
        }
        running = true
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            bind(msm.getActiveSessions(component).firstOrNull())
        }.onFailure { onChanged(null) }
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun bind(c: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback, main)
        publish()
    }

    fun current(): Info? {
        val c = controller ?: return null
        val md = c.metadata
        val st = c.playbackState
        val actions = st?.actions ?: 0L
        // Vị trí tính tiếp từ lần cập nhật cuối nếu đang phát (PlaybackState chỉ báo theo mốc).
        var pos = st?.position ?: 0L
        if (st?.state == PlaybackState.STATE_PLAYING) {
            pos += ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
        }
        val dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        return Info(
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = st?.state == PlaybackState.STATE_PLAYING,
            position = if (dur > 0) pos.coerceIn(0, dur) else pos.coerceAtLeast(0),
            duration = dur,
            hasNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            hasPrev = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            appPackage = c.packageName
        )
    }

    private fun publish() = onChanged(current())

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
    fun seekTo(ms: Long) = controller?.transportControls?.seekTo(ms)

    // Mở app đang phát nhạc (bấm ảnh bìa) qua sessionActivity nếu có.
    fun openPlayer(context: Context): Boolean {
        val c = controller ?: return false
        val pi = c.sessionActivity
        if (pi != null && CcActions.sendPendingIntent(pi)) return true
        val launch = context.packageManager.getLaunchIntentForPackage(c.packageName) ?: return false
        return CcActions.start(context, launch)
    }
}
