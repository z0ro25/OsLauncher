package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemInstallIconBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemInstallIconPageBinding

// Các trang lưới app (giống desktop) trong tab Icon của màn Install; vuốt ngang để xem hết icon.
// Hàng cao cố định, xếp từ trên xuống; mỗi trang tối đa columns x rows icon như desktop.
class InstallIconPageAdapter(
    private val pages: List<List<ThemeLogoItem>>,
    private val columns: Int,
    private val rows: Int
) : RecyclerView.Adapter<InstallIconPageAdapter.PageHolder>() {

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = ThemesItemInstallIconPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.rvPageIcons.layoutManager = object : GridLayoutManager(parent.context, columns) {
            override fun canScrollVertically() = false
        }
        return PageHolder(binding)
    }

    // Gắn adapter ngay; KHÔNG đợi doOnLayout: setAdapter trong onLayoutChange gọi requestLayout giữa
    // lúc ViewPager2 đang layout nên bị nuốt -> lưới không bao giờ vẽ icon.
    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.binding.rvPageIcons.adapter = IconAdapter(pages[position], columns, rows)
    }

    class PageHolder(val binding: ThemesItemInstallIconPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class IconAdapter(
        private val items: List<ThemeLogoItem>,
        private val columns: Int,
        private val rows: Int
    ) : RecyclerView.Adapter<IconAdapter.Holder>() {

        override fun getItemCount() = items.size

        // Hàng cao cố định (icon + tên + khe) nên các hàng xếp sát nhau từ TRÊN xuống như desktop,
        // không giãn phủ kín khung. Lưới cao không đủ [rows] hàng thì mới co icon lại cho vừa.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ThemesItemInstallIconBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            val res = parent.resources
            val maxIcon = res.getDimensionPixelSize(R.dimen.themes_install_icon_size)
            val labelSpace = res.getDimensionPixelSize(R.dimen.themes_install_label_space)
            val colGap = res.getDimensionPixelSize(R.dimen.themes_install_icon_gap)
            val rowGap = res.getDimensionPixelSize(R.dimen.themes_install_row_gap)

            val colWidth = (parent.width - parent.paddingLeft - parent.paddingRight) / columns
            val inner = parent.height - parent.paddingTop - parent.paddingBottom
            val byWidth = if (colWidth > 0) colWidth - colGap else maxIcon
            val byHeight = if (inner > 0) inner / rows - labelSpace - rowGap else maxIcon
            val icon = minOf(maxIcon, byWidth, byHeight).coerceAtLeast(maxIcon / 2)

            binding.root.layoutParams.height = icon + labelSpace + rowGap
            binding.root.setPadding(0, 0, 0, 0)
            binding.imgAppIcon.layoutParams.width = icon
            binding.imgAppIcon.layoutParams.height = icon
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            Glide.with(holder.binding.imgAppIcon).load(item.imageUrl).into(holder.binding.imgAppIcon)
            holder.binding.tvAppName.text = item.label
        }

        class Holder(val binding: ThemesItemInstallIconBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
