package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.PreviewCard
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemPreviewCardBinding
import com.google.android.material.shape.ShapeAppearanceModel

// Thẻ preview theme/wallpaper; [small] = thẻ hàng Suggest (102:196, bo 11) thay vì thẻ lớn.
class PreviewCardAdapter(
    private val small: Boolean,
    private val onClick: (PreviewCard) -> Unit
) : RecyclerView.Adapter<PreviewCardAdapter.Holder>() {

    private val items = mutableListOf<PreviewCard>()

    fun submit(list: List<PreviewCard>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ThemesItemPreviewCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (small) {
            (binding.imgCard.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "102:196"
            binding.imgCard.shapeAppearanceModel = ShapeAppearanceModel.builder(
                parent.context, 0, R.style.ThemesRoundedImage11
            ).build()
        }
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        Glide.with(holder.binding.imgCard).load(item.image).into(holder.binding.imgCard)
        holder.binding.imgCard.contentDescription = item.name
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemPreviewCardBinding) : RecyclerView.ViewHolder(binding.root)
}
