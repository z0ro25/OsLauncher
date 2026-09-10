package com.ezla.oslauncher.Features.wallpaperonboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.databinding.ItemSelectBackgroundBinding

/**
 * Adapter cho ViewPager2 màn Chọn hình nền (onboarding).
 * Mỗi item là 1 ảnh nằm trong assets/wallpapers/, nạp bằng Glide qua uri file:///android_asset.
 *
 * [assetPaths]: đường dẫn asset tương đối tính từ gốc assets, ví dụ "wallpapers/img_1.webp".
 * Người dùng chỉ cần thả ảnh vào app/assets/wallpapers/ là danh sách tự có (xem SelectBackgroundActivity).
 */
class SelectBackgroundAdapter(
    private val context: Context,
    private val assetPaths: List<String>
) : RecyclerView.Adapter<SelectBackgroundAdapter.WallpaperViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        return WallpaperViewHolder(
            ItemSelectBackgroundBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = assetPaths.size

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        // Glide đọc trực tiếp từ assets; không giữ bitmap gốc trong RAM nên carousel mượt.
        Glide.with(context)
            .load("file:///android_asset/${assetPaths[position]}")
            .centerCrop()
            .into(holder.binding.ivWallpaper)
    }

    class WallpaperViewHolder(val binding: ItemSelectBackgroundBinding) :
        RecyclerView.ViewHolder(binding.root)
}
