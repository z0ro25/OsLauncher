package com.oslauncher.applauncher.themelauncher.Features.wallpaper.selectwallpaper.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderYourWallpaperBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper

class VPWallpaperAdapter(val context: Context, val wallpapers: ArrayList<YourWallpaper>) :
    RecyclerView.Adapter<ViewHolder>() {
    var onLockCustomClick: ((YourWallpaper) -> Unit)? = null
    var onHomeCustomClick: ((YourWallpaper) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        return YourWallpaperViewHolder(
            ViewholderYourWallpaperBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun getItemCount(): Int = wallpapers.size

    override fun onBindViewHolder(viewhoder: ViewHolder, post: Int) {
        val data = wallpapers[post]
        val wpVh = viewhoder as YourWallpaperViewHolder
        wpVh.binding.apply {
            if (!data.homeBGPath.isNullOrEmpty()) {
                val bm = BitmapFactory.decodeFile(data.homeBGPath)
                ivBgHome.setImageBitmap(bm)
            } else {
                ivBgHome.setImageResource(R.drawable.bg_hello)
            }

            if (!data.lockBGPath.isNullOrEmpty()) {
                val bm = BitmapFactory.decodeFile(data.lockBGPath)
                ivBgLock.setImageBitmap(bm)
            } else {
                ivBgLock.setImageResource(R.drawable.bg_hello)
            }

            btnCustomLock.tap {
                onLockCustomClick?.invoke(data)
            }

            btnCustomHome.tap {
                onHomeCustomClick?.invoke(data)
            }
        }
    }

    class YourWallpaperViewHolder(val binding: ViewholderYourWallpaperBinding) :
        ViewHolder(binding.root)
}