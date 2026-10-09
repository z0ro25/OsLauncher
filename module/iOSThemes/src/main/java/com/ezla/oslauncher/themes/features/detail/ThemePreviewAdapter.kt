package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.databinding.ThemesItemThemePreviewIconsBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemThemePreviewWallpaperBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration

// Adapter cho ViewPager2 ở màn Install theme: trang 0 = wallpaper, trang 1 = lưới icon.
// Vuốt ngang để user xem theme gồm những gì.
class ThemePreviewAdapter(
    private val wallpaperUrl: String?,
    private val iconUrls: List<String>,
    private val gapPx: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = PAGE_COUNT

    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == PAGE_WALLPAPER) {
            WallpaperHolder(
                ThemesItemThemePreviewWallpaperBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else {
            IconsHolder(
                ThemesItemThemePreviewIconsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WallpaperHolder -> Glide.with(holder.binding.imgWallpaperPreview)
                .load(wallpaperUrl).into(holder.binding.imgWallpaperPreview)

            is IconsHolder -> {
                val ctx = holder.binding.rvIcons.context
                holder.binding.rvIcons.layoutManager = GridLayoutManager(ctx, ICON_SPAN)
                holder.binding.rvIcons.addItemDecoration(GridSpacingDecoration(ICON_SPAN, gapPx))
                holder.binding.rvIcons.adapter = ThemeIconPreviewAdapter(iconUrls)
            }
        }
    }

    class WallpaperHolder(val binding: ThemesItemThemePreviewWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root)

    class IconsHolder(val binding: ThemesItemThemePreviewIconsBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val PAGE_WALLPAPER = 0
        const val PAGE_ICONS = 1
        private const val PAGE_COUNT = 2
        private const val ICON_SPAN = 4
    }
}
