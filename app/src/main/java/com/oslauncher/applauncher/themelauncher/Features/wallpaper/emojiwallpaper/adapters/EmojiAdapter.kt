package com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderEmojiBinding
import com.oslauncher.applauncher.themelauncher.model.EmojiModel

class EmojiAdapter(val context: Context, val items: ArrayList<EmojiModel>) : Adapter<ViewHolder>() {
    var onEmojiClick: ((EmojiModel, Int) -> Unit)? = null
    var onDeleteClick: ((EmojiModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return EmojiViewHolder(
            ViewholderEmojiBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        val hd = holder as EmojiViewHolder
        hd.binding.apply {
            tvEmoji.text = data.emoji

            icClose.isVisible = data.isEdit
            selectView.isVisible = data.isEdit

            root.setOnClickListener {
                onEmojiClick?.invoke(data,position)
            }

            icClose.setOnClickListener {
                onDeleteClick?.invoke(data)
            }
        }
    }

    class EmojiViewHolder(val binding: ViewholderEmojiBinding) : ViewHolder(binding.root)

}