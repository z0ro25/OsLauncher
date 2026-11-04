package com.ezla.oslauncher.controlcenter

import android.content.Context
import android.content.SharedPreferences
import com.ezla.oslauncher.controlcenter.data.CcEdge
import com.ezla.oslauncher.controlcenter.data.CcSlot

// Cài đặt Control Center. File prefs riêng của module để service lắng nghe thay đổi (listener)
// mà không bị kích bởi key của module khác.
object CcPrefs {

    private const val FILE = "control_center"

    const val KEY_ENABLED = "enabled"
    const val KEY_BLUR = "blur"
    const val KEY_VIBRATION = "vibration"
    const val KEY_EDGE = "edge"
    const val KEY_EDGE_COLOR = "edge_color"
    const val KEY_EDGE_HIDE = "edge_hide"
    const val KEY_EDGE_WIDTH = "edge_width"
    const val KEY_EDGE_LENGTH = "edge_length"
    const val KEY_EDGE_OPACITY = "edge_opacity"
    const val KEY_EDGE_OFFSET = "edge_offset"
    const val KEY_TILES = "tiles"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_GUIDE_SHOWN = "guide_shown"

    // Thanh trượt Edge Settings lưu 0..100; mặc định giữa thanh như Figma.
    const val SLIDER_DEFAULT = 50

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isOnboardingDone(c: Context) = prefs(c).getBoolean(KEY_ONBOARDING_DONE, false)
    fun markOnboardingDone(c: Context) = prefs(c).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()

    fun isGuideShown(c: Context) = prefs(c).getBoolean(KEY_GUIDE_SHOWN, false)
    fun markGuideShown(c: Context) = prefs(c).edit().putBoolean(KEY_GUIDE_SHOWN, true).apply()

    fun isEnabled(c: Context) = prefs(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_ENABLED, v).apply()

    fun isBlur(c: Context) = prefs(c).getBoolean(KEY_BLUR, true)
    fun setBlur(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_BLUR, v).apply()

    fun isVibration(c: Context) = prefs(c).getBoolean(KEY_VIBRATION, false)
    fun setVibration(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_VIBRATION, v).apply()

    fun edgeSettings(c: Context): EdgeSettings {
        val p = prefs(c)
        return EdgeSettings(
            edge = CcEdge.fromName(p.getString(KEY_EDGE, null)),
            color = p.getInt(KEY_EDGE_COLOR, EdgeSettings.DEFAULT_COLOR),
            hide = p.getBoolean(KEY_EDGE_HIDE, false),
            width = p.getInt(KEY_EDGE_WIDTH, SLIDER_DEFAULT),
            length = p.getInt(KEY_EDGE_LENGTH, SLIDER_DEFAULT),
            opacity = p.getInt(KEY_EDGE_OPACITY, SLIDER_DEFAULT),
            offset = p.getInt(KEY_EDGE_OFFSET, SLIDER_DEFAULT)
        )
    }

    fun saveEdgeSettings(c: Context, s: EdgeSettings) {
        prefs(c).edit()
            .putString(KEY_EDGE, s.edge.name)
            .putInt(KEY_EDGE_COLOR, s.color)
            .putBoolean(KEY_EDGE_HIDE, s.hide)
            .putInt(KEY_EDGE_WIDTH, s.width)
            .putInt(KEY_EDGE_LENGTH, s.length)
            .putInt(KEY_EDGE_OPACITY, s.opacity)
            .putInt(KEY_EDGE_OFFSET, s.offset)
            .apply()
    }

    // Ô hệ thống + ô app (CcSlot.key); đọc được cả định dạng cũ chỉ có tên CcTile, bỏ qua mục lạ.
    fun tiles(c: Context): List<CcSlot> {
        val raw = prefs(c).getString(KEY_TILES, null) ?: return CcSlot.DEFAULT
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').mapNotNull { CcSlot.fromKey(it) }.distinct()
    }

    fun saveTiles(c: Context, tiles: List<CcSlot>) {
        prefs(c).edit().putString(KEY_TILES, tiles.joinToString(",") { it.key }).apply()
    }

    data class EdgeSettings(
        val edge: CcEdge,
        val color: Int,
        val hide: Boolean,
        val width: Int,
        val length: Int,
        val opacity: Int,
        val offset: Int
    ) {
        companion object {
            const val DEFAULT_COLOR = 0xFF000000.toInt()
        }
    }
}
