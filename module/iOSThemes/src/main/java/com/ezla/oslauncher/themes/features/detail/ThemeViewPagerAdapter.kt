package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.databinding.ThemesItemViewHomeBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemViewLockBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Carousel màn View theme: trái = background, giữa = màn khoá (nền + giờ), phải = thumbnail.
// Mặc định đứng ở trang giữa (PAGE_LOCK).
class ThemeViewPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var backgroundUrl: String? = null
    private var thumbnailUrl: String? = null

    fun submit(background: String?, thumbnail: String?) {
        backgroundUrl = background
        thumbnailUrl = thumbnail
        notifyDataSetChanged()
    }

    override fun getItemCount() = PAGE_COUNT

    override fun getItemViewType(position: Int) = if (position == PAGE_LOCK) TYPE_LOCK else TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_LOCK) {
            LockHolder(ThemesItemViewLockBinding.inflate(inflater, parent, false))
        } else {
            ImageHolder(ThemesItemViewHomeBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageHolder -> {
                val url = if (position == PAGE_BACKGROUND) backgroundUrl else thumbnailUrl
                Glide.with(holder.binding.imgHome).load(url).into(holder.binding.imgHome)
            }
            is LockHolder -> {
                Glide.with(holder.binding.imgLock).load(backgroundUrl).into(holder.binding.imgLock)
                val now = Date()
                holder.binding.tvLockDate.text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
                holder.binding.tvLockTime.text = SimpleDateFormat("H:mm", Locale.getDefault()).format(now)
            }
        }
    }

    class ImageHolder(val binding: ThemesItemViewHomeBinding) : RecyclerView.ViewHolder(binding.root)
    class LockHolder(val binding: ThemesItemViewLockBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val PAGE_BACKGROUND = 0
        const val PAGE_LOCK = 1
        private const val PAGE_COUNT = 3
        private const val TYPE_IMAGE = 0
        private const val TYPE_LOCK = 1
    }
}
