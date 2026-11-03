package com.ezla.oslauncher.themes.features.rewards

import android.view.LayoutInflater
import android.view.View
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivityRewardsBinding

// Màn Rewards / My Coin (Figma "Rewards") mở từ badge xu / Not Enough Coins ở các màn chi tiết.
// Nội dung + logic dùng chung với tab Rewards của Home qua RewardsController.
class RewardsActivity : BaseActivity<ThemesActivityRewardsBinding>() {
    override val setViewBinding: ThemesActivityRewardsBinding
        get() = ThemesActivityRewardsBinding.inflate(LayoutInflater.from(this))

    var callback: RewardsCallback = object : RewardsCallback {}

    private val controller by lazy { RewardsController(this, this, binding.content, callback) }

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.themes_rewards_title)
        binding.header.imgHeaderAction.apply {
            setImageResource(R.drawable.themes_ic_help)
            contentDescription = getString(R.string.themes_cd_help)
            visibility = View.VISIBLE
        }
        controller.setup()
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
        binding.header.imgHeaderAction.setOnClickListener { controller.toggleTooltip() }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onPause() {
        controller.onPause()
        super.onPause()
    }
}
