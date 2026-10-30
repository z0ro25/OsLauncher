package com.ezla.oslauncher.calculator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lịch sử phép tính, mới nhất ở đầu. Lưu JSON trong prefs riêng của module, giới hạn MAX_ITEMS. */
class HistoryStore(context: Context) {

    data class Entry(val id: Long, val expression: String, val result: String, val time: Long)

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): MutableList<Entry> {
        val raw = prefs.getString(KEY, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Entry(o.getLong("id"), o.getString("e"), o.getString("r"), o.getLong("t"))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun add(expression: String, result: String) {
        val list = load()
        val now = System.currentTimeMillis()
        list.add(0, Entry(now, expression, result, now))
        save(list.take(MAX_ITEMS))
    }

    fun delete(ids: Set<Long>) = save(load().filterNot { it.id in ids })

    fun clear() = prefs.edit().remove(KEY).apply()

    private fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("e", it.expression).put("r", it.result).put("t", it.time))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "calc_history"
        private const val KEY = "items"
        private const val MAX_ITEMS = 100
    }
}
