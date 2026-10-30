package com.ezla.oslauncher.measure.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Measurement(val id: Long, val meters: Float)

// Lịch sử đo (sheet "Measurements") lưu prefs riêng của module, mới nhất ở đầu.
class MeasureStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<Measurement> {
        val arr = runCatching { JSONArray(prefs.getString(KEY_ITEMS, "[]")) }.getOrElse { JSONArray() }
        return (0 until arr.length()).mapNotNull {
            val o = arr.getJSONObject(it)
            // Bỏ qua bản ghi loại khác đoạn thẳng ("t" != line) từ bản thử nghiệm đo hình chữ nhật.
            val type = o.optString("t", TYPE_LINE)
            if (type != TYPE_LINE) null else Measurement(o.getLong("id"), o.getDouble("m").toFloat())
        }
    }

    fun add(meters: Float) {
        val list = listOf(Measurement(System.currentTimeMillis(), meters)) + all()
        save(list.take(MAX_ITEMS))
    }

    fun clear() = save(emptyList())

    private fun save(list: List<Measurement>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("m", it.meters.toDouble())) }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    private companion object {
        const val PREFS = "measure_prefs"
        const val KEY_ITEMS = "items"
        const val TYPE_LINE = "line"
        const val MAX_ITEMS = 50
    }
}
