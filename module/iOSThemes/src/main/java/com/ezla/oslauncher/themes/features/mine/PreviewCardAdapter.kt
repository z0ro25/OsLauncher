package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.PreviewCard
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.features.loadCardImage
import com.ezla.oslauncher.themes.databinding.ThemesItemPreviewCardBinding
import com.google.android.material.shape.ShapeAppearanceModel

// Thẻ preview theme/wallpaper; [small] = thẻ hàng Suggest (320:690 centerCrop, bo 11) thay vì thẻ lớn.
// [showNewBadge]: chỉ tab Wallpapers bật nhãn New; Mine/Suggest dùng chung adapter nên mặc định tắt.
class PreviewCardAdapter(
    private val small: Boolean,
    private val showNewBadge: Boolean = false,
    private val onClick: (PreviewCard) -> Unit
) : RecyclerView.Adapter<PreviewCardAdapter.Holder>() {

    private val items = mutableListOf<PreviewCard>()

    fun submit(list: List<PreviewCard>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    // Loadmore (chip All tab Wallpapers): nối vào cuối, giữ vị trí cuộn và các thẻ đã bind.
    fun append(list: List<PreviewCard>) {
        if (list.isEmpty()) return
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ThemesItemPreviewCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (small) {
            // Cùng tỉ lệ + centerCrop với thumbnail tab Themes (themes_item_theme) để ảnh Suggest không méo.
            (binding.imgCard.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "320:690"
            binding.imgCard.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.imgCard.shapeAppearanceModel = ShapeAppearanceModel.builder(
                parent.context, 0, R.style.ThemesRoundedImage11
            ).build()
        }
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    // Ảnh tải lỗi -> gỡ thẻ. Tìm theo đúng object (không theo vị trí) vì list có thể đã đổi.
    private fun removeItem(item: PreviewCard) {
        val index = items.indexOfFirst { it === item }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.pbImage.visibility = View.VISIBLE
        holder.binding.imgCard.loadCardImage(item.image) { ok ->
            // Holder đã bị tái dùng cho thẻ khác thì không đụng vòng quay của thẻ mới.
            if (items.getOrNull(holder.bindingAdapterPosition) === item) holder.binding.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        holder.binding.imgCard.contentDescription = item.name
        holder.binding.tvNew.visibility =
            if (showNewBadge && !item.isUnlocked && !item.isDefault) View.VISIBLE else View.GONE
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemPreviewCardBinding) : RecyclerView.ViewHolder(binding.root)
}
