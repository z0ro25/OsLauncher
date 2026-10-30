package com.ezla.oslauncher.clock.ui

import android.content.Context
import android.media.Ringtone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.ring.ClockSounds
import kotlin.concurrent.thread

// Danh sách chuông (Figma "Sound" / "When Timer Ends"): tick trái, bấm = chọn + nghe thử.
// [withStop]: thêm "Stop Playing" (im lặng) cuối danh sách cho hẹn giờ.
class SoundPicker(
    private val context: Context,
    private val list: RecyclerView,
    private val withStop: Boolean,
    private val onPicked: (String) -> Unit
) {
    var selected: String = ""
        private set
    private var sounds: List<ClockSounds.Sound> = emptyList()
    private var playing: Ringtone? = null

    private val adapter: TextRowAdapter = TextRowAdapter(checkAtStart = true) { i ->
        val s = sounds.getOrNull(i) ?: return@TextRowAdapter
        selected = s.uri
        render()
        stopPreview()
        playing = ClockSounds.preview(context, s.uri)
        onPicked(s.uri)
    }

    init {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    /** Đọc RingtoneManager ở nền (cursor MediaStore có thể chậm vài trăm ms). */
    fun load(current: String) {
        selected = current
        thread(name = "osclock-sounds") {
            val all = ClockSounds.list(context).let {
                if (withStop) it + ClockSounds.Sound(ClockSounds.SILENT, context.getString(R.string.osclock_stop_playing)) else it
            }
            list.post {
                sounds = all
                render()
            }
        }
    }

    private fun render(): Unit = adapter.submit(sounds.map { it.title }) { sounds[it].uri == selected }

    fun stopPreview() {
        playing?.runCatching { stop() }
        playing = null
    }
}
