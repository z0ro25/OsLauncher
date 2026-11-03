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
        // Lưới đo xong / đổi cỡ -> tính lại 1 cỡ chung rồi vẽ lại cả trang (post: không gọi giữa lúc layout).
        binding.rvPageIcons.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val rv = v as RecyclerView
            val current = rv.adapter as? IconAdapter ?: return@addOnLayoutChangeListener
            if (current.fitTo(rv)) rv.post { current.notifyDataSetChanged() }
        }
        return PageHolder(binding)
    }

    // Gắn adapter ngay; KHÔNG đợi doOnLayout: setAdapter trong onLayoutChange gọi requestLayout giữa
    // lúc ViewPager2 đang layout nên bị nuốt -> lưới không bao giờ vẽ icon.
    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val rv = holder.binding.rvPageIcons
        val adapter = IconAdapter(pages[position], columns, rows)
        rv.adapter = adapter
        adapter.fitTo(rv)
    }

    class PageHolder(val binding: ThemesItemInstallIconPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class IconAdapter(
        private val items: List<ThemeLogoItem>,
        private val columns: Int,
        private val rows: Int
    ) : RecyclerView.Adapter<IconAdapter.Holder>() {

        // Cỡ icon chung cho CẢ trang. Trước đây tính lúc tạo từng ô: ô hàng đầu tạo khi lưới chưa đo
        // (width = 0) nên lấy cỡ tối đa, ô sau tính theo lưới đã đo -> hàng đầu to hơn hàng dưới.
        private var icon = 0
        private var labelSpace = 0
        private var rowGap = 0

        override fun getItemCount() = items.size

        // Tính cỡ theo kích thước lưới hiện tại; lưới chưa đo thì tạm dùng cỡ tối đa. true = cỡ vừa đổi.
        fun fitTo(parent: RecyclerView): Boolean {
            val res = parent.resources
            val maxIcon = res.getDimensionPixelSize(R.dimen.themes_install_icon_size)
            labelSpace = res.getDimensionPixelSize(R.dimen.themes_install_label_space)
            rowGap = res.getDimensionPixelSize(R.dimen.themes_install_row_gap)
            val colGap = res.getDimensionPixelSize(R.dimen.themes_install_icon_gap)

            val colWidth = (parent.width - parent.paddingLeft - parent.paddingRight) / columns
            val inner = parent.height - parent.paddingTop - parent.paddingBottom
            val byWidth = if (colWidth > 0) colWidth - colGap else maxIcon
            val byHeight = if (inner > 0) inner / rows - labelSpace - rowGap else maxIcon
            val size = minOf(maxIcon, byWidth, byHeight).coerceAtLeast(maxIcon / 2)
            if (size == icon) return false
            icon = size
            return true
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ThemesItemInstallIconBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            binding.root.setPadding(0, 0, 0, 0)
            return Holder(binding)
        }

        // Hàng cao cố định (icon + tên + khe) nên các hàng xếp sát nhau từ TRÊN xuống như desktop.
        // Áp cỡ ở bind (không ở create) để mọi ô, kể cả ô tạo sớm, luôn cùng 1 cỡ hiện tại.
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val b = holder.binding
            b.root.layoutParams = b.root.layoutParams.apply { height = icon + labelSpace + rowGap }
            b.imgAppIcon.layoutParams = b.imgAppIcon.layoutParams.apply {
                width = icon
                height = icon
            }
            val item = items[position]
            Glide.with(b.imgAppIcon).load(item.imageUrl).into(b.imgAppIcon)
            b.tvAppName.text = item.label
        }

        class Holder(val binding: ThemesItemInstallIconBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
