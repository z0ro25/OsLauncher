package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.HomeKind
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.databinding.ThemesItemHomeCardBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemIconCardBinding
import com.ezla.oslauncher.themes.features.CardImageTracker
import com.ezla.oslauncher.themes.features.controlcenter.CcPreviewBinder
import com.ezla.oslauncher.themes.features.icons.IconCardAdapter
import com.ezla.oslauncher.themes.features.icons.IconCellAdapter
import com.ezla.oslauncher.themes.features.loadCardImage

// Hàng thẻ ngang của 1 section tab Themes. [items] null = đang tải -> [PLACEHOLDERS] thẻ chờ.
// Theme / Wallpaper / Control Center dùng thẻ dọc 118x228; Icon dùng lại thẻ của tab Icons cũ.
// Ảnh thẻ lỗi -> gỡ thẻ (như các tab cũ); hết thẻ thì báo [onEmpty] để gỡ cả section.
class HomeCardAdapter(
    private val kind: HomeKind,
    items: List<Any>?,
    private val onClick: (Any) -> Unit,
    private val onEmpty: () -> Unit,
    // Lưới See more: thẻ lấp bề ngang ô (giữ tỉ lệ 320:690) thay vì rộng cố định của hàng ngang.
    private val grid: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val loading = items == null
    private val items = items.orEmpty().toMutableList()

    override fun getItemViewType(position: Int) = when {
        loading -> TYPE_PLACEHOLDER
        kind == HomeKind.ICON -> TYPE_ICON
        else -> TYPE_CARD
    }

    override fun getItemCount(): Int = if (loading) PLACEHOLDERS else items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ICON) {
            val b = ThemesItemIconCardBinding.inflate(inflater, parent, false)
            b.rvIcons.layoutManager = object : GridLayoutManager(parent.context, IconCardAdapter.COLUMNS) {
                override fun canScrollVertically() = false
                override fun canScrollHorizontally() = false
            }
            IconHolder(b)
        } else {
            val b = ThemesItemHomeCardBinding.inflate(inflater, parent, false)
            if (grid) b.root.layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            CardHolder(b)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IconHolder -> bindIcon(holder, items[position] as IconThemeCard)
            is CardHolder -> if (loading) bindPlaceholder(holder) else bindCard(holder, items[position])
        }
    }

    private fun bindPlaceholder(h: CardHolder) {
        val b = h.binding
        h.bound = null
        Glide.with(b.imgCard).clear(b.imgCard)
        b.imgPlaceholder.visibility = View.VISIBLE
        b.frCcPreview.visibility = View.GONE
        b.pbImage.visibility = View.GONE
        b.tvNew.visibility = View.GONE
        b.root.setOnClickListener(null)
    }

    private fun bindCard(h: CardHolder, item: Any) {
        val b = h.binding
        h.bound = item
        b.imgPlaceholder.visibility = View.GONE
        val image: Any?
        val isNew: Boolean
        when (item) {
            is ThemeItem -> {
                image = item.previewUrl
                isNew = !item.isUnlocked && !DefaultThemeProvider.isDefault(item.id)
            }
            is PreviewCard -> {
                image = item.image
                isNew = !item.isUnlocked && !item.isDefault
            }
            is ControlCenterCard -> {
                image = item.background
                isNew = false
            }
            else -> return
        }
        b.tvNew.visibility = if (isNew) View.VISIBLE else View.GONE
        // Control Center: nền theme + panel phủ lên; thẻ xong khi cả nền lẫn mọi ảnh skin đã tải.
        val cc = item as? ControlCenterCard
        b.frCcPreview.visibility = if (cc != null) View.VISIBLE else View.GONE
        val tracker = CardImageTracker((if (image != null) 1 else 0) + (if (cc != null) 1 else 0)) { ok ->
            if (h.bound !== item) return@CardImageTracker
            b.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        b.pbImage.visibility = if (tracker.done) View.GONE else View.VISIBLE
        // Thumbnail theme đúng tỉ lệ khung 320:690 -> fitXY để hiện trọn ảnh; nền wallpaper / Control
        // Center khác tỉ lệ nên giữ centerCrop cho khỏi méo.
        b.imgCard.scaleType = if (item is ThemeItem) ImageView.ScaleType.FIT_XY else ImageView.ScaleType.CENTER_CROP
        if (image != null) b.imgCard.loadCardImage(image, tracker::report)
        else Glide.with(b.imgCard).clear(b.imgCard)
        if (cc != null) h.ccPreview().bind(cc.skins, cc.colors, tracker::report)
        b.root.setOnClickListener { onClick(item) }
    }

    private fun bindIcon(h: IconHolder, item: IconThemeCard) {
        val b = h.binding
        h.bound = item
        val tracker = CardImageTracker(item.icons.size + if (item.background != null) 1 else 0) { ok ->
            if (h.bound !== item) return@CardImageTracker
            b.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        b.pbImage.visibility = if (tracker.done) View.GONE else View.VISIBLE
        if (item.background != null) b.imgBackground.loadCardImage(item.background, tracker::report)
        else Glide.with(b.imgBackground).clear(b.imgBackground)
        b.rvIcons.adapter = IconCellAdapter(item.icons, tracker::report)
        b.tvName.text = item.name
        b.tvNew.visibility =
            if (item.isUnlocked || DefaultThemeProvider.isDefault(item.id)) View.GONE else View.VISIBLE
        b.root.contentDescription = item.name
        // RecyclerView lưới icon luôn nuốt chạm -> nhận click qua lớp phủ viewClick + phần tên.
        b.viewClick.setOnClickListener { onClick(item) }
        b.root.setOnClickListener { onClick(item) }
    }

    // Tìm theo đúng object (không theo vị trí) vì list có thể đã đổi.
    private fun removeItem(item: Any) {
        val index = items.indexOfFirst { it === item }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
        if (items.isEmpty()) onEmpty()
    }

    class CardHolder(val binding: ThemesItemHomeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        var bound: Any? = null
        private var cc: CcPreviewBinder? = null

        // Panel xem trước tốn view: chỉ dựng khi holder thật sự hiển thị thẻ Control Center.
        fun ccPreview(): CcPreviewBinder = cc ?: CcPreviewBinder(binding.frCcPreview).also { cc = it }
    }

    class IconHolder(val binding: ThemesItemIconCardBinding) : RecyclerView.ViewHolder(binding.root) {
        var bound: IconThemeCard? = null
    }

    companion object {
        private const val TYPE_CARD = 0
        private const val TYPE_ICON = 1
        private const val TYPE_PLACEHOLDER = 2
        private const val PLACEHOLDERS = 3
    }
}
