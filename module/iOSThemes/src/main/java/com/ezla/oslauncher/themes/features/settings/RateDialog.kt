package com.ezla.oslauncher.themes.features.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogRateBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemStarBinding

// Dialog Rate us (Figma "Rate us"). Mặc định 4 sao như thiết kế; gửi số sao qua [onRate].
class RateDialog(context: Context, private val onRate: (Int) -> Unit) :
    Dialog(context, R.style.ThemesDialog) {

    private var stars = DEFAULT_STARS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ThemesDialogRateBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        val starAdapter = StarAdapter()
        binding.rvStars.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStars.adapter = starAdapter

        binding.imgClose.setOnClickListener { dismiss() }
        binding.btnRateNow.setOnClickListener {
            dismiss()
            onRate(stars)
        }
    }

    private inner class StarAdapter : RecyclerView.Adapter<StarAdapter.StarHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = StarHolder(
            ThemesItemStarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun getItemCount(): Int = MAX_STARS

        override fun onBindViewHolder(holder: StarHolder, position: Int) {
            holder.binding.imgStar.setImageResource(
                if (position < stars) R.drawable.themes_ic_star_on else R.drawable.themes_ic_star_off
            )
            holder.binding.imgStar.setOnClickListener {
                stars = position + 1
                notifyDataSetChanged()
            }
        }

        inner class StarHolder(val binding: ThemesItemStarBinding) : RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        private const val MAX_STARS = 5
        private const val DEFAULT_STARS = 4
    }
}
