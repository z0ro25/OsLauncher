package com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.EmojiWallpaperActivity
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderEmojiWallpaperBinding
import com.oslauncher.applauncher.themelauncher.utils.EmojiWallpaperUtils

class EmojiWallpaperAdapter : Adapter<ViewHolder>() {
    val listEmoji: ArrayList<String> = arrayListOf()
    var bm: Bitmap? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return WallpaperEmojiViewHolder(
            ViewholderEmojiWallpaperBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun getItemCount(): Int = EmojiWallpaperUtils.EmojiWallpaperType.values().size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hd = holder as WallpaperEmojiViewHolder
        val width = EmojiWallpaperActivity.width
        val height = EmojiWallpaperActivity.height

        hd.binding.apply {
            when (position) {
                EmojiWallpaperUtils.EmojiWallpaperType.SMALL_GRID.ordinal -> {
                    bm = EmojiWallpaperUtils.generateGridWallpaper(listEmoji, width, height)
                }

                EmojiWallpaperUtils.EmojiWallpaperType.MEDIUM_GRID.ordinal -> {
                    bm = EmojiWallpaperUtils.generateMediumGridPattern(listEmoji, width, height)
                }

                EmojiWallpaperUtils.EmojiWallpaperType.LARGE_GRID.ordinal -> {
                    bm = EmojiWallpaperUtils.generateLargeGridPattern(listEmoji, width, height)
                }

                EmojiWallpaperUtils.EmojiWallpaperType.RING.ordinal -> {
                    bm = EmojiWallpaperUtils.generateRingPattern(listEmoji, width, height)
                }

                EmojiWallpaperUtils.EmojiWallpaperType.SPIRAL.ordinal -> {
                    bm =
                        EmojiWallpaperUtils.generateMultiSpiralPattern(listEmoji, width, height)
                }
            }
            ivWallpaper.setImageBitmap(bm)
        }
    }

    class WallpaperEmojiViewHolder(val binding: ViewholderEmojiWallpaperBinding) :
        ViewHolder(binding.root)
}