package com.ezla.oslauncher.controlcenter.skin

import android.content.Context
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcSlot
import com.ezla.oslauncher.controlcenter.data.CcTile
import com.ezla.oslauncher.controlcenter.databinding.CcPanelMainBinding
import com.ezla.oslauncher.controlcenter.panel.TileGridAdapter
import com.ezla.oslauncher.controlcenter.panel.TileGridDecoration

// Trang chính của panel dựng tĩnh để xem trước skin (thẻ / màn chi tiết ở module Themes): không nghe
// hệ thống, không nhận chạm, mọi ô tắt (trừ khi setAccent). Rộng cố định cc_panel_width; nơi dùng tự thu nhỏ.
class CcPanelPreview(context: Context) {

    private val themed = ContextThemeWrapper(context, R.style.CcTheme)
    private val binding = CcPanelMainBinding.inflate(LayoutInflater.from(themed))
    private var accent: CcAccent? = null
    // Có màu theme thì ô đèn pin hiện trạng thái bật để thấy màu; không có thì mọi ô tắt như cũ.
    private val tiles = TileGridAdapter(isOn = { accent != null && it == CcTile.FLASHLIGHT }, onClick = {})

    // Bọc chặn chạm (click rơi về view cha) và luôn đo đúng kích thước thật của panel, không bị cha
    // ép hẹp lại; nơi dùng tự scale view này cho vừa khung.
    val view: View = object : FrameLayout(themed) {
        override fun onInterceptTouchEvent(ev: MotionEvent) = true
        override fun onTouchEvent(event: MotionEvent) = false
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val free = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            super.onMeasure(free, free)
        }
    }

    init {
        val b = binding
        val res = themed.resources
        b.ccTileGrid.layoutManager = object : GridLayoutManager(themed, SPAN) {
            override fun canScrollVertically() = false
        }
        b.ccTileGrid.addItemDecoration(TileGridDecoration(
            res.getDimensionPixelSize(R.dimen.cc_panel_width),
            res.getDimensionPixelSize(R.dimen.cc_tile),
            res.getDimensionPixelSize(R.dimen.cc_tile_row_gap)
        ))
        b.ccTileGrid.itemAnimator = null
        b.ccTileGrid.adapter = tiles
        tiles.tiles = CcSlot.DEFAULT
        b.ccTileRotation.setIcon(R.drawable.cc_ic_rotation_lock)
        b.ccTileSilent.setIcon(R.drawable.cc_ic_bell_slash)
        b.ccSliderBrightness.icon = ContextCompat.getDrawable(themed, R.drawable.cc_ic_sun)
        b.ccSliderVolume.icon = ContextCompat.getDrawable(themed, R.drawable.cc_ic_volume)
        b.ccSliderBrightness.setValue(BRIGHTNESS)
        b.ccSliderVolume.setValue(VOLUME)
        b.ccSliderBrightness.isLocked = true
        b.ccSliderVolume.isLocked = true
        b.ccNpTitle.setText(R.string.cc_track)
        b.ccNpArtist.setText(R.string.cc_artist)
        (view as ViewGroup).addView(b.root, FrameLayout.LayoutParams(
            res.getDimensionPixelSize(R.dimen.cc_panel_width), ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    // null = nền kính mặc định (Control Center gốc của launcher).
    fun setSkin(source: CcSkinSource?) {
        binding.applySkin(themed, source ?: CcSkinSource { null })
        tiles.skin = source
    }

    // Màu "đang bật" của theme để xem trước: bật ô khoá xoay + đèn pin để thấy màu; null = như cũ.
    fun setAccent(accent: CcAccent?) {
        this.accent = accent
        val b = binding
        b.ccTileRotation.isOn = accent != null
        b.ccTileRotation.accent = accent
        b.ccTileSilent.accent = accent
        b.ccSliderBrightness.accent = accent
        b.ccSliderVolume.accent = accent
        tiles.accent = accent
        listOf(b.ccChipWifi, b.ccChipBluetooth, b.ccChipCellular).forEach { chip ->
            chip.background = accent?.drawable() ?: ContextCompat.getDrawable(themed,
                if (chip === b.ccChipCellular) R.drawable.cc_bg_chip_green else R.drawable.cc_bg_chip_blue)
            chip.imageTintList = ColorStateList.valueOf(accent?.iconColor ?: 0xFFFFFFFF.toInt())
        }
    }

    private companion object {
        const val SPAN = 4
        const val BRIGHTNESS = 0.7f
        const val VOLUME = 0.5f
    }
}
