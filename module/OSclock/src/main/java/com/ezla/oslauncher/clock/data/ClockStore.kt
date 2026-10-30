package com.ezla.oslauncher.clock.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.TimeZone

// Toàn bộ dữ liệu Clock nằm trong 1 file prefs riêng: receiver lúc boot đọc lại được mà
// không cần mở Activity. Mọi hàm đều đồng bộ (dữ liệu vài KB).
class ClockStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Alarms ----

    fun alarms(): List<Alarm> = readArray(KEY_ALARMS).map { Alarm.fromJson(it) }

    fun alarm(id: Int): Alarm? = alarms().firstOrNull { it.id == id }

    fun saveAlarm(a: Alarm) {
        val list = alarms().filter { it.id != a.id } + a
        writeArray(KEY_ALARMS, list.map { it.toJson() })
    }

    fun deleteAlarm(id: Int) = writeArray(KEY_ALARMS, alarms().filter { it.id != id }.map { it.toJson() })

    fun sleepAlarm(): Alarm? = alarms().firstOrNull { it.isSleep }

    var sleepBedMinutes: Int
        get() = prefs.getInt(KEY_SLEEP_BED, 23 * 60)
        set(v) = prefs.edit().putInt(KEY_SLEEP_BED, v).apply()

    // ---- Timers ----

    fun timers(): List<ClockTimer> = readArray(KEY_TIMERS).map { ClockTimer.fromJson(it) }

    fun timer(id: Int): ClockTimer? = timers().firstOrNull { it.id == id }

    fun saveTimer(t: ClockTimer) {
        val list = timers().toMutableList()
        val i = list.indexOfFirst { it.id == t.id }
        if (i >= 0) list[i] = t else list.add(0, t)
        writeArray(KEY_TIMERS, list.map { it.toJson() })
    }

    fun deleteTimer(id: Int) = writeArray(KEY_TIMERS, timers().filter { it.id != id }.map { it.toJson() })

    /** Thời lượng đã dùng gần đây (ms), mới nhất trước, không trùng. */
    fun recents(): List<Long> {
        val arr = JSONArray(prefs.getString(KEY_RECENTS, "[]"))
        return (0 until arr.length()).map { arr.getLong(it) }
    }

    fun addRecent(ms: Long) {
        val list = (listOf(ms) + recents().filter { it != ms }).take(MAX_RECENTS)
        prefs.edit().putString(KEY_RECENTS, JSONArray(list).toString()).apply()
    }

    fun deleteRecent(ms: Long) {
        prefs.edit().putString(KEY_RECENTS, JSONArray(recents().filter { it != ms }).toString()).apply()
    }

    var timerLabel: String
        get() = prefs.getString(KEY_TIMER_LABEL, "") ?: ""
        set(v) = prefs.edit().putString(KEY_TIMER_LABEL, v).apply()

    var timerSound: String
        get() = prefs.getString(KEY_TIMER_SOUND, "") ?: ""
        set(v) = prefs.edit().putString(KEY_TIMER_SOUND, v).apply()

    var pickerDurationMs: Long
        get() = prefs.getLong(KEY_PICKER_DURATION, 10 * 60_000L)
        set(v) = prefs.edit().putLong(KEY_PICKER_DURATION, v).apply()

    // ---- World clock ----

    /** Lần đầu: chỉ có múi giờ của máy (như iOS). */
    fun cities(): List<String> {
        val raw = prefs.getString(KEY_CITIES, null)
            ?: return listOf(TimeZone.getDefault().id).also { saveCities(it) }
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun saveCities(ids: List<String>) {
        prefs.edit().putString(KEY_CITIES, JSONArray(ids).toString()).apply()
    }

    // ---- Stopwatch (mốc wall-clock để sống qua lần tắt app) ----

    var swStartedAt: Long
        get() = prefs.getLong(KEY_SW_START, 0L)
        set(v) = prefs.edit().putLong(KEY_SW_START, v).apply()

    var swAccumulated: Long
        get() = prefs.getLong(KEY_SW_ACC, 0L)
        set(v) = prefs.edit().putLong(KEY_SW_ACC, v).apply()

    /** Tổng thời gian tại thời điểm bấm Lap (cộng dồn), cũ nhất trước. */
    var swLapMarks: List<Long>
        get() {
            val arr = JSONArray(prefs.getString(KEY_SW_LAPS, "[]"))
            return (0 until arr.length()).map { arr.getLong(it) }
        }
        set(v) = prefs.edit().putString(KEY_SW_LAPS, JSONArray(v).toString()).apply()

    var swPage: Int
        get() = prefs.getInt(KEY_SW_PAGE, 0)
        set(v) = prefs.edit().putInt(KEY_SW_PAGE, v).apply()

    var askedExact: Boolean
        get() = prefs.getBoolean(KEY_ASKED_EXACT, false)
        set(v) = prefs.edit().putBoolean(KEY_ASKED_EXACT, v).apply()

    var lastTab: Int
        get() = prefs.getInt(KEY_TAB, 0)
        set(v) = prefs.edit().putInt(KEY_TAB, v).apply()

    fun nextId(): Int {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    private fun readArray(key: String) = JSONArray(prefs.getString(key, "[]")).let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun writeArray(key: String, items: List<org.json.JSONObject>) {
        prefs.edit().putString(key, JSONArray(items).toString()).apply()
    }

    companion object {
        private const val PREFS = "osclock_store"
        private const val KEY_ALARMS = "alarms"
        private const val KEY_TIMERS = "timers"
        private const val KEY_RECENTS = "timer_recents"
        private const val KEY_TIMER_LABEL = "timer_label"
        private const val KEY_TIMER_SOUND = "timer_sound"
        private const val KEY_PICKER_DURATION = "timer_picker"
        private const val KEY_CITIES = "cities"
        private const val KEY_SW_START = "sw_start"
        private const val KEY_SW_ACC = "sw_acc"
        private const val KEY_SW_LAPS = "sw_laps"
        private const val KEY_SW_PAGE = "sw_page"
        private const val KEY_SLEEP_BED = "sleep_bed"
        private const val KEY_TAB = "tab"
        private const val KEY_ASKED_EXACT = "asked_exact"
        private const val KEY_NEXT_ID = "next_id"
        private const val MAX_RECENTS = 10
    }
}
