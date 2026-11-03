package com.ezla.oslauncher.controlcenter.skin

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcPanelMainBinding

// Nguồn ảnh nền ô: panel thật đọc file đã cài (CcSkin), preview ở module Themes đọc ảnh tải từ URL.
// Trả drawable MỚI mỗi lần gọi; null = ô đó dùng nền kính mặc định.
fun interface CcSkinSource {
    fun get(slot: CcSkinSlot): Drawable?

    companion object {
        fun installed(c: Context) = CcSkinSource { CcSkin.drawable(c, it) }

        fun of(c: Context, map: Map<CcSkinSlot, Drawable>) = CcSkinSource { slot ->
            map[slot]?.constantState?.newDrawable(c.resources)?.mutate()
        }
    }
}

// Gắn nền cho các khối cố định của trang chính; lưới ô 1x1 lấy nền qua TileGridAdapter.skin.
internal fun CcPanelMainBinding.applySkin(context: Context, source: CcSkinSource) {
    fun set(v: View, slot: CcSkinSlot, def: Int) {
        v.background = source.get(slot) ?: ContextCompat.getDrawable(context, def)
    }
    set(ccConnectivity, CcSkinSlot.TILE_2X2, R.drawable.cc_bg_tile_big)
    set(ccNowPlaying, CcSkinSlot.TILE_2X2, R.drawable.cc_bg_tile_big)
    set(ccFocus, CcSkinSlot.TILE_2X1, R.drawable.cc_bg_tile)
    ccSliderBrightness.skin = source.get(CcSkinSlot.TILE_1X2)
    ccSliderVolume.skin = source.get(CcSkinSlot.TILE_1X2)
    ccTileRotation.skin = source
    ccTileSilent.skin = source
}
