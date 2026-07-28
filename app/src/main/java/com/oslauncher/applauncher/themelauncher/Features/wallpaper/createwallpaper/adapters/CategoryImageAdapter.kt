package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderItemImageBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class CategoryImageAdapter(val context: Context, val listImage: List<String>) :
    Adapter<ViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        return CategoryImageViewHolder(
            ViewholderItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = listImage.size

    override fun onBindViewHolder(vhd: ViewHolder, post: Int) {
        val data = listImage[post]
        val holder = vhd as CategoryImageViewHolder
        holder.binding.apply {
            loading.isVisible = true
            Glide.with(context)
                .asBitmap()
                .load(YourWallpaperDataManager.IMAGE_SERVER_PATH + data)
                .listener(object : RequestListener<Bitmap>{
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
                onItemClick?.invoke(data)
            }
        }
    }

    class CategoryImageViewHolder(val binding: ViewholderItemImageBinding) :
        ViewHolder(binding.root)
}