package com.ezla.oslauncher.controlcenter.panel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcTile
import com.ezla.oslauncher.controlcenter.databinding.CcItemTileBinding
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource

// Lưới ô 1x1. Chế độ sửa: mỗi ô có nút "–" và kéo thả đổi chỗ được, sau các ô là 1 ô "+"
// và các slot trống tới đủ 12.
class TileGridAdapter(
    private val isOn: (CcTile) -> Boolean,
    private val onClick: (CcTile) -> Unit,
    private val onRemove: (CcTile) -> Unit = {},
    private val onAdd: () -> Unit = {}
) : RecyclerView.Adapter<TileGridAdapter.Holder>() {

    var tiles: List<CcTile> = emptyList()
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    // Thứ tự đang kéo: đổi chỗ bằng notifyItemMoved (notifyDataSetChanged sẽ huỷ thao tác kéo).
    private var dragging: MutableList<CcTile>? = null
    private val current get() = dragging ?: tiles

    var editMode = false
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    // Ảnh nền ô 1x1 của theme đang áp; null = nền kính mặc định.
    var skin: CcSkinSource? = null
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    // Màu "đang bật" của theme cho ô 1x1; null = nền trắng như cũ.
    var accent: CcAccent? = null
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    // Chỉ ô có chức năng mới kéo được / thả vào được; ô "+" và slot trống đứng yên ở cuối.
    fun isMovable(position: Int) = editMode && position in current.indices

    fun move(from: Int, to: Int): Boolean {
        if (!isMovable(from) || !isMovable(to)) return false
        val list = dragging ?: tiles.toMutableList().also { dragging = it }
        list.add(to, list.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    // Thả tay: trả thứ tự mới (null = không đổi) để nơi gắn lưu, rồi vẽ lại cho nút "–" đúng ô.
    fun endDrag(): List<CcTile>? {
        val result = dragging?.toList()
        dragging = null
        if (result != null && result != tiles) {
            tiles = result
            return result
        }
        return null
    }

    class Holder(val b: CcItemTileBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemCount() = if (editMode) CcTile.MAX else current.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(CcItemTileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: Holder, position: Int) {
        val list = current
        val tile = list.getOrNull(position)
        val b = h.b
        b.ccTile.skin = skin
        b.ccTile.accent = accent
        when {
            tile != null -> {
                b.ccTile.isSlot = false
                b.ccTile.setIcon(tile.icon)
                b.ccTile.isOn = !editMode && isOn(tile)
                b.ccTile.contentDescription = b.root.context.getString(tile.label)
                b.ccTile.setOnClickListener { if (!editMode) onClick(tile) }
                b.ccTileMinus.visibility = if (editMode) View.VISIBLE else View.GONE
                b.ccTileMinus.setOnClickListener { onRemove(tile) }
            }
            position == list.size && list.size < CcTile.MAX -> {
                b.ccTile.isSlot = false
                b.ccTile.setIcon(R.drawable.cc_ic_plus)
                b.ccTile.isOn = false
                b.ccTile.contentDescription = b.root.context.getString(R.string.cc_add_control)
                b.ccTile.setOnClickListener { onAdd() }
                b.ccTileMinus.visibility = View.GONE
            }
            else -> {
                b.ccTile.isOn = false
                b.ccTile.setOnClickListener(null)
                b.ccTile.isSlot = true
                b.ccTileMinus.visibility = View.GONE
            }
        }
    }
}
