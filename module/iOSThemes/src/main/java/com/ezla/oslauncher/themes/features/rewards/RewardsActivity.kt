package com.ezla.oslauncher.themes.features.rewards

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivityRewardsBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration

// Màn Rewards / My Coin (Figma "Rewards"). Chỉ dựng UI; hành động đi qua RewardsCallback, chưa nối.
// Các dialog (RewardDialog, CoinToastDialog) dựng sẵn, chưa gắn vào luồng nào.
class RewardsActivity : BaseActivity<ThemesActivityRewardsBinding>() {
    override val setViewBinding: ThemesActivityRewardsBinding
        get() = ThemesActivityRewardsBinding.inflate(LayoutInflater.from(this))

    var callback: RewardsCallback = object : RewardsCallback {}

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.themes_rewards_title)
        binding.header.imgHeaderAction.apply {
            setImageResource(R.drawable.themes_ic_help)
            contentDescription = getString(R.string.themes_cd_help)
            visibility = View.VISIBLE
        }
        binding.tvVideoCoin.text = getString(R.string.themes_coin_plus, RewardsSample.VIDEO_COINS)

        val days = RewardsSample.days()
        binding.rvCheckin.layoutManager = noScrollGrid(CHECKIN_SPAN).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == days.lastIndex) 2 else 1
            }
        }
        binding.rvCheckin.addItemDecoration(SpanAwareSpacingDecoration(dp(8)))
        binding.rvCheckin.adapter = CheckinAdapter(days) { callback.onCheckinDayClick(this, it) }

        binding.rvPacks.layoutManager = noScrollGrid(PACK_SPAN)
        binding.rvPacks.addItemDecoration(GridSpacingDecoration(PACK_SPAN, dp(15)))
        binding.rvPacks.adapter = CoinPackAdapter(RewardsSample.packs()) { callback.onPackClick(this, it) }
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
        binding.header.imgHeaderAction.setOnClickListener { toggleTooltip() }
        binding.layoutTooltip.setOnClickListener { toggleTooltip() }
        binding.layoutWatchVideo.setOnClickListener { callback.onWatchVideoClick(this) }
    }

    override fun dataObservable() {}

    private fun toggleTooltip() {
        binding.layoutTooltip.visibility =
            if (binding.layoutTooltip.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun noScrollGrid(span: Int) = object : GridLayoutManager(this, span) {
        override fun canScrollVertically() = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHECKIN_SPAN = 4
        private const val PACK_SPAN = 3
    }
}
