package com.ezla.oslauncher.controlcenter.panel

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcItemConnectionBinding
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSlot
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource

// Trang Kết nối (Figma "Screen Control Center/Network"): 6 dòng, bấm mở màn cài đặt tương ứng.
class ConnectionAdapter(private val onClick: (Item) -> Unit) :
    RecyclerView.Adapter<ConnectionAdapter.Holder>() {

    enum class Item(@DrawableRes val icon: Int, @StringRes val title: Int, @DrawableRes val onBg: Int) {
        AIRPLANE(R.drawable.cc_ic_airplane, R.string.cc_airplane, R.drawable.cc_bg_chip_blue),
        WIFI(R.drawable.cc_ic_wifi, R.string.cc_wifi, R.drawable.cc_bg_chip_blue),
        CELLULAR(R.drawable.cc_ic_cellular, R.string.cc_cellular, R.drawable.cc_bg_chip_green),
        BLUETOOTH(R.drawable.cc_ic_bluetooth, R.string.cc_bluetooth, R.drawable.cc_bg_chip_blue),
        HOTSPOT(R.drawable.cc_ic_hotspot, R.string.cc_hotspot, R.drawable.cc_bg_chip_blue),
        SYNC(R.drawable.cc_ic_sync, R.string.cc_sync, R.drawable.cc_bg_chip_blue)
    }

    var states: Map<Item, Boolean> = emptyMap()
        set(v) {
            if (field == v) return
            field = v
            notifyDataSetChanged()
        }

    // Dòng 300x60 nhận ảnh nền 1x4 của theme; null = nền kính mặc định.
    var skin: CcSkinSource? = null
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    // Màu "đang bật" của theme thay xanh dương / xanh lá của chip; null = màu iOS gốc.
    var accent: CcAccent? = null
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    class Holder(val b: CcItemConnectionBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemCount() = Item.values().size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(CcItemConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: Holder, position: Int) {
        val item = Item.values()[position]
        val on = states[item] == true
        h.b.ccConnIcon.setImageResource(item.icon)
        val onAccent = if (on) accent else null
        if (onAccent != null) h.b.ccConnIcon.background = onAccent.drawable()
        else h.b.ccConnIcon.setBackgroundResource(if (on) item.onBg else R.drawable.cc_bg_chip_off)
        h.b.ccConnIcon.imageTintList = ColorStateList.valueOf(onAccent?.iconColor ?: 0xFFFFFFFF.toInt())
        h.b.ccConnTitle.setText(item.title)
        h.b.ccConnDetail.setText(if (on) R.string.cc_on else R.string.cc_off)
        val bg = skin?.get(CcSkinSlot.TILE_1X4)
        if (bg != null) h.b.root.background = bg else h.b.root.setBackgroundResource(R.drawable.cc_bg_tile)
        h.b.root.setOnClickListener { onClick(item) }
    }
}
