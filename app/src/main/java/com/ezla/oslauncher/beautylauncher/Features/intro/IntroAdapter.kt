package com.ezla.oslauncher.beautylauncher.Features.intro

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ezla.oslauncher.beautylauncher.model.IntroModel
import com.ezla.oslauncher.beautylauncher.databinding.ItemSlideLayoutBinding

class IntroAdapter(val context: Context, val introItems: List<IntroModel>) : Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return IntroViewHolder(
            ItemSlideLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = introItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = introItems[position]
        val viewholder = holder as IntroViewHolder
        viewholder.binding.apply {
            data.bmID?.let { imLogoSlide.setImageResource(it) }
        }
    }

    override fun getItemViewType(position: Int): Int = 0

    private class IntroViewHolder(val binding: ItemSlideLayoutBinding) : ViewHolder(binding.root)
}
