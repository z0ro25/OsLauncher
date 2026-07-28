package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.adapters

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderItemAllImageBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class SeeAllAdapter(val context: Context, val allItems: List<String>) : Adapter<ViewHolder>() {
    var onImageClick : ((String) ->Unit) ? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ItemAllImageViewHolder(
            ViewholderItemAllImageBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun getItemCount(): Int = allItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = allItems[position]
        val viewHolder = holder as ItemAllImageViewHolder
        viewHolder.binding.apply {
            loading.isVisible = true
            Glide.with(context)
                .asBitmap()
                .load(YourWallpaperDataManager.IMAGE_SERVER_PATH + data)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        p0: GlideException?,
                        p1: Any?,
                        p2: Target<Bitmap>?,
                        p3: Boolean
                    ): Boolean {
                        loading.isVisible = false
                        return false
                    }

                    override fun onResourceReady(
                        p0: Bitmap?,
                        p1: Any?,
                        p2: Target<Bitmap>?,
                        p3: DataSource?,
                        p4: Boolean
                    ): Boolean {
                        loading.isVisible = false
                        return false
                    }

                }).into(ivPhoto)

            root.tap {
                onImageClick?.invoke(data)
            }
        }

    }

    class ItemAllImageViewHolder(val binding: ViewholderItemAllImageBinding) :
        ViewHolder(binding.root)
}