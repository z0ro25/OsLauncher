package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderListImageBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.LIstImageModel
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class AllServerImgAdapter(val context: Context, val photos: ArrayList<LIstImageModel>) :
    Adapter<ViewHolder>() {

    var onImageClick: ((String) -> Unit)? = null
    var onSeeAllClick: ((LIstImageModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        return ListImageViewHolder(
            ViewholderListImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = photos.size

    override fun getItemViewType(position: Int): Int = 0

    override fun onBindViewHolder(vhd: ViewHolder, post: Int) {
        val data = photos[post]
        val viewholdet = vhd as ListImageViewHolder
        viewholdet.binding.apply {
            tvCategoryName.text = context.getString(
                YourWallpaperDataManager.PhotoCategory.values()
                    .lastOrNull { it.nameString == data.categoryName }?.nameId
                    ?: R.string.weather
            )

            val categoryImages = data.listImage?.split(",")
            categoryImages?.let {
                val adapter = CategoryImageAdapter(context, it)
                rcvListImage.adapter = adapter
                adapter.onItemClick = onImageClick
            }

            tvSeeAll.tap {
                onSeeAllClick?.invoke(data)
            }
        }
    }

    class ListImageViewHolder(val binding: ViewholderListImageBinding) : ViewHolder(binding.root)
}
