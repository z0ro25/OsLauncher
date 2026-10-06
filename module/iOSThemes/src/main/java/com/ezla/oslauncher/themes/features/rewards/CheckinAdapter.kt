package com.ezla.oslauncher.themes.features.rewards

import com.ezla.oslauncher.themes.Models.CheckinDay
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemCheckinDayBinding

// Lưới 4 cột; ngày cuối (7) chiếm 2 cột và có túi tiền -> xem spanSizeLookup ở RewardsActivity.
class CheckinAdapter(
    private val days: List<CheckinDay>,
    private val onClick: (CheckinDay) -> Unit
) : RecyclerView.Adapter<CheckinAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemCheckinDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val day = days[position]
        val isLast = position == days.lastIndex
        val ctx = holder.itemView.context
        holder.binding.apply {
            tvDay.text = ctx.getString(R.string.themes_checkin_day, day.day)
            tvDayCoin.text = ctx.getString(R.string.themes_coin_amount, day.coins)
            layoutDay.setBackgroundResource(if (day.isToday) R.drawable.themes_bg_day_today else R.drawable.themes_bg_day)
            imgPurse.visibility = if (isLast) View.VISIBLE else View.GONE
            (layoutDayContent.layoutParams as FrameLayout.LayoutParams).gravity =
                if (isLast) Gravity.START else Gravity.CENTER_HORIZONTAL
            root.setOnClickListener { onClick(day) }
        }
    }

    class Holder(val binding: ThemesItemCheckinDayBinding) : RecyclerView.ViewHolder(binding.root)
}
