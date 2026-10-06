package com.ezla.oslauncher.themes.features.rewards

import com.ezla.oslauncher.themes.Models.CoinPack
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemCoinPackBinding

// Gói đầu (Figma): nền gradient vàng-cam + giá chữ trắng; các gói còn lại nền vàng + giá nâu.
class CoinPackAdapter(
    private val packs: List<CoinPack>,
    private val onClick: (CoinPack) -> Unit
) : RecyclerView.Adapter<CoinPackAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemCoinPackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = packs.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pack = packs[position]
        val featured = position == 0
        holder.binding.apply {
            layoutPack.setBackgroundResource(if (featured) R.drawable.themes_bg_pack_featured else R.drawable.themes_bg_pack)
            tvPackCoins.text = pack.coins.toString()
            imgPackCoins.setImageResource(pack.icon)
            tvPackPrice.text = pack.price
            tvPackPrice.setTextColor(if (featured) Color.WHITE else PRICE_COLOR)
            root.setOnClickListener { onClick(pack) }
        }
    }

    class Holder(val binding: ThemesItemCoinPackBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val PRICE_COLOR = Color.parseColor("#524500")
    }
}
