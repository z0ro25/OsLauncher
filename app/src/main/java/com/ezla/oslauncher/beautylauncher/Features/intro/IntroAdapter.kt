package com.ezla.oslauncher.beautylauncher.Features.intro

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ezla.oslauncher.beautylauncher.databinding.ItemSlideLayoutBinding
import com.ezla.oslauncher.beautylauncher.databinding.ViewholderNativeFullOnbBinding
import com.ezla.oslauncher.beautylauncher.extensions.layoutInflater
import com.ezla.oslauncher.beautylauncher.model.IntroModel
import com.truongnt.ios.ioslite.common.R
import com.truongnt.ios.ioslite.common.ads.AdsNative
import com.truongnt.ios.ioslite.common.ads.AdsNativeCallback
import com.truongnt.ios.ioslite.common.ads.AdsSlot

class IntroAdapter(val context: Context, val introItems: List<IntroModel>) : Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) IntroViewHolder(
            ItemSlideLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) else NativeFullOnbViewHolder(
            ViewholderNativeFullOnbBinding.inflate(context.layoutInflater, parent, false)
        )
    }

    override fun getItemCount(): Int = introItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = introItems[position]
        if (data.position == "native_full") {
            (holder as NativeFullOnbViewHolder).apply {
                AdsNative.show(
                    binding.ntFullContainer,
                    AdsSlot.NATIVE_ONBOARDING_FULL,
                    R.layout.layout_native_full,
                    object : AdsNativeCallback() {

                    })
            }

        } else {
            val viewholder = holder as IntroViewHolder
            viewholder.binding.apply {
                data.bmID?.let { imLogoSlide.setImageResource(it) }
            }
        }

    }

    override fun getItemViewType(position: Int): Int =
        if (introItems[position].position == "native_full") 1 else 0

    private class IntroViewHolder(val binding: ItemSlideLayoutBinding) : ViewHolder(binding.root)
    private class NativeFullOnbViewHolder(val binding: ViewholderNativeFullOnbBinding) :
        ViewHolder(binding.root)
}
