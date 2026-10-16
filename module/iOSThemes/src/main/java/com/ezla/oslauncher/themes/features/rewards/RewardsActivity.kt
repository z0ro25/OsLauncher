package com.ezla.oslauncher.themes.features.rewards

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.Models.CoinPack
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.data.CoinPacks
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DailyRewardStore
import com.truongnt.ios.ioslite.common.config.AppAds
import com.ezla.oslauncher.themes.databinding.ThemesActivityRewardsBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.launch

// Màn Rewards / My Coin (Figma "Rewards"). Gói xu = IAP thật (CoinPacks, billing ở :app), mua xong
// toast "+N Coins". Xem video: rewarded ad, đủ 3 lượt/ngày +5 xu (DailyRewardStore). Check-in chưa nối.
class RewardsActivity : BaseActivity<ThemesActivityRewardsBinding>() {
    override val setViewBinding: ThemesActivityRewardsBinding
        get() = ThemesActivityRewardsBinding.inflate(LayoutInflater.from(this))

    var callback: RewardsCallback = object : RewardsCallback {}

    // Đang xem ad / đang cộng xu: chặn bấm lặp.
    private var watching = false

    private val packAdapter =CoinPackAdapter(CoinPacks.packs()) { buyPack(it) }
    private val pricesListener: () -> Unit = { packAdapter.submit(CoinPacks.packs()) }
    private val coinsListener: (Int, Int) -> Unit = { coins, _ ->
        if (!isFinishing && !isDestroyed) CoinToastDialog(this, coins).show()
    }

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.themes_rewards_title)
        binding.header.imgHeaderAction.apply {
            setImageResource(R.drawable.themes_ic_help)
            contentDescription = getString(R.string.themes_cd_help)
            visibility = View.VISIBLE
        }
        binding.tvVideoCoin.text = getString(R.string.themes_coin_plus, DailyRewardStore.COINS)
        renderVideoProgress()
        AppAds.preloadRewarded(REWARDED_KEY)

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
        binding.rvPacks.adapter = packAdapter
        CoinPacks.onPricesLoaded = pricesListener
        CoinPacks.onCoinsAdded = coinsListener
    }

    // Bấm gói -> luồng mua của Play (billing ở :app); xu cộng xong báo về qua onCoinsAdded.
    private fun buyPack(pack: CoinPack) {
        callback.onPackClick(this, pack)
        if (!CoinPacks.launchPurchase(this, pack.id)) {
            Toast.makeText(this, R.string.themes_store_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Chỉ gỡ khi callback còn là của màn này (màn Rewards mới có thể đã gắn đè).
        if (CoinPacks.onPricesLoaded === pricesListener) CoinPacks.onPricesLoaded = null
        if (CoinPacks.onCoinsAdded === coinsListener) CoinPacks.onCoinsAdded = null
        super.onDestroy()
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
        binding.header.imgHeaderAction.setOnClickListener { toggleTooltip() }
        binding.layoutTooltip.setOnClickListener { toggleTooltip() }
        binding.layoutWatchVideo.setOnClickListener {
            callback.onWatchVideoClick(this)
            watchVideo()
        }
        binding.btnTestAddCoin.setOnClickListener { testAddCoin() }
    }

    // CHỈ ĐỂ TEST POST /credits: mỗi lần bấm cộng TEST_COINS; khoá nút tới khi có kết quả
    // để không gửi trùng. Gỡ cùng nút btnTestAddCoin trước khi release.
    private fun testAddCoin() {
        binding.btnTestAddCoin.isEnabled = false
        lifecycleScope.launch {
            CreditRepository.addCredit(TEST_COINS, TEST_TYPE)
                .onSuccess {
                    Toast.makeText(this@RewardsActivity, getString(R.string.themes_test_add_coin_done, TEST_COINS, it), Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(this@RewardsActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                }
            binding.btnTestAddCoin.isEnabled = true
        }
    }

    override fun dataObservable() {}

    // Xem video hằng ngày: mỗi lượt xem đủ ghi 1 lần; đủ VIEWS_PER_DAY thì cộng COINS xu (1 lần/ngày).
    private fun watchVideo() {
        if (watching) return
        if (DailyRewardStore.isRewardPending(this)) {
            claimDailyReward()
            return
        }
        if (!DailyRewardStore.canWatch(this)) {
            Toast.makeText(this, R.string.themes_reward_done_today, Toast.LENGTH_SHORT).show()
            return
        }
        watching = true
        AppAds.showRewarded(this, REWARDED_KEY) { earned ->
            watching = false
            if (isFinishing || isDestroyed) return@showRewarded
            if (!earned) {
                Toast.makeText(this, R.string.themes_reward_no_ad, Toast.LENGTH_SHORT).show()
                return@showRewarded
            }
            DailyRewardStore.addView(this)
            renderVideoProgress()
            if (DailyRewardStore.isRewardPending(this)) claimDailyReward() else AppAds.preloadRewarded(REWARDED_KEY)
        }
    }

    // Cộng xu lên server; chỉ đánh dấu đã nhận khi OK, lỗi thì bấm lại để cộng (không phải xem lại).
    private fun claimDailyReward() {
        watching = true
        lifecycleScope.launch {
            CreditRepository.addCredit(DailyRewardStore.COINS, DailyRewardStore.CREDIT_TYPE)
                .onSuccess {
                    DailyRewardStore.markClaimed(this@RewardsActivity)
                    CoinToastDialog(this@RewardsActivity, DailyRewardStore.COINS).show()
                }
                .onFailure {
                    Toast.makeText(this@RewardsActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                }
            watching = false
            renderVideoProgress()
        }
    }

    // "Hôm nay x/3"; đã nhận thưởng hôm nay thì làm mờ hàng (sang ngày mới tự mở lại).
    private fun renderVideoProgress() {
        binding.tvVideoProgress.text = getString(
            R.string.themes_reward_progress, DailyRewardStore.views(this), DailyRewardStore.VIEWS_PER_DAY
        )
        binding.layoutWatchVideo.alpha = if (DailyRewardStore.isClaimed(this)) DONE_ALPHA else 1f
    }

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
        private const val TEST_COINS = 50
        private const val TEST_TYPE = "view_ads"
        // Placement rewarded trong app/assets/ads/*placements.json.
        private const val REWARDED_KEY = "rewarded_coin"
        private const val DONE_ALPHA = 0.5f
    }
}
