package com.ezla.oslauncher.themes.features.rewards

import android.os.Handler
import android.os.Looper
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
import com.ezla.oslauncher.themes.data.DailyCheckinStore
import com.ezla.oslauncher.themes.data.DailyRewardStore
import com.truongnt.ios.ioslite.common.config.AppAds
import com.ezla.oslauncher.themes.databinding.ThemesActivityRewardsBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.launch
import java.util.Locale

// Màn Rewards / My Coin (Figma "Rewards"). Gói xu = IAP thật (CoinPacks, billing ở :app), mua xong
// toast "+N Coins". Xem video: rewarded ad, mỗi lượt +1 xu, 5 lượt/ngày, cách nhau 5 phút (DailyRewardStore).
// Check-in: bấm ô hôm nay để nhận (DailyCheckinStore, dùng chung CheckinDialog).
class RewardsActivity : BaseActivity<ThemesActivityRewardsBinding>() {
    override val setViewBinding: ThemesActivityRewardsBinding
        get() = ThemesActivityRewardsBinding.inflate(LayoutInflater.from(this))

    var callback: RewardsCallback = object : RewardsCallback {}

    // Đang xem ad / đang cộng xu: chặn bấm lặp.
    private var watching = false
    private var claimingCheckin = false
    private var checkinAdapter: CheckinAdapter? = null

    private val packAdapter = CoinPackAdapter(CoinPacks.packs()) { buyPack(it) }
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
        binding.tvVideoCoin.text = getString(R.string.themes_coin_plus, DailyRewardStore.COINS_PER_VIEW)
        renderVideoProgress()
        AppAds.preloadRewarded(REWARDED_KEY)

        checkinAdapter = CheckinBinder.bindGrid(binding.checkinCard.rvCheckin) { day ->
            callback.onCheckinDayClick(this, day)
            if (day.isToday && !day.claimed) claimCheckin()
        }

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
    }

    override fun dataObservable() {}

    // Bấm ô hôm nay: nhận xu check-in (cùng luồng nút Claim ở dialog), xong làm mờ ô.
    private fun claimCheckin() {
        if (claimingCheckin) return
        claimingCheckin = true
        lifecycleScope.launch {
            val result = CheckinBinder.claimToday(this@RewardsActivity)
            claimingCheckin = false
            if (result == null || isFinishing || isDestroyed) return@launch
            checkinAdapter?.submit(DailyCheckinStore.days(this@RewardsActivity))
            RewardDialog(this@RewardsActivity, RewardDialog.Type.CHECKIN, result.first).show()
        }
    }

    // Mở lại màn sau khi đã nhận ở dialog / sang ngày mới -> làm mới trạng thái 7 ô.
    // Kèm xử lý giao dịch xu bị kẹt trước khi user bấm mua (kẹt thì Play chặn mua lại).
    override fun onResume() {
        super.onResume()
        screenResumed = true
        renderVideoProgress()
        checkinAdapter?.submit(DailyCheckinStore.days(this))
        CoinPacks.syncPending()
    }

    // Xem video: mỗi lượt được thưởng +COINS_PER_VIEW xu ngay, tối đa VIEWS_PER_DAY lượt/ngày,
    // 2 lượt cách nhau COOLDOWN_MS. Còn nợ thưởng (lỗi mạng) thì bấm là cộng bù, không xem lại.
    private fun watchVideo() {
        if (watching) return
        if (DailyRewardStore.isRewardPending(this)) {
            claimPendingReward()
            return
        }
        if (DailyRewardStore.views(this) >= DailyRewardStore.VIEWS_PER_DAY) {
            Toast.makeText(this, R.string.themes_reward_done_today, Toast.LENGTH_SHORT).show()
            return
        }
        val left = DailyRewardStore.cooldownLeftMs(this)
        if (left > 0L) {
            Toast.makeText(this, getString(R.string.themes_reward_wait_toast, formatMmSs(left)), Toast.LENGTH_SHORT).show()
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
            claimPendingReward()
            AppAds.preloadRewarded(REWARDED_KEY)
        }
    }

    // Cộng toàn bộ lượt còn nợ trong 1 lần gọi; chỉ trừ nợ khi server OK, lỗi thì lần bấm sau cộng lại.
    private fun claimPendingReward() {
        val count = DailyRewardStore.pending(this)
        if (count <= 0) return
        val coins = count * DailyRewardStore.COINS_PER_VIEW
        watching = true
        lifecycleScope.launch {
            CreditRepository.addCredit(coins, DailyRewardStore.CREDIT_TYPE)
                .onSuccess {
                    DailyRewardStore.markClaimed(this@RewardsActivity, count)
                    if (!isFinishing && !isDestroyed) CoinToastDialog(this@RewardsActivity, coins).show()
                }
                .onFailure {
                    Toast.makeText(this@RewardsActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                }
            watching = false
            renderVideoProgress()
        }
    }

    // Dòng phụ: đang chờ -> "Next video in mm:ss" (đếm từng giây), không thì "Today x/5".
    // Hết lượt hôm nay (và không nợ thưởng) thì làm mờ hàng; sang ngày mới tự mở lại.
    private fun renderVideoProgress() {
        cooldownHandler.removeCallbacks(cooldownTick)
        val views = DailyRewardStore.views(this)
        val left = DailyRewardStore.cooldownLeftMs(this)
        val waiting = left > 0L && views < DailyRewardStore.VIEWS_PER_DAY && !DailyRewardStore.isRewardPending(this)
        binding.tvVideoProgress.text = if (waiting) {
            getString(R.string.themes_reward_cooldown, formatMmSs(left))
        } else {
            getString(R.string.themes_reward_progress, views, DailyRewardStore.VIEWS_PER_DAY)
        }
        binding.layoutWatchVideo.alpha =
            if (waiting || DailyRewardStore.isDoneToday(this)) DONE_ALPHA else 1f
        // Chỉ chạy đồng hồ khi màn đang hiện (dừng ở onPause), tránh tick nền vô ích.
        if (waiting && screenResumed) cooldownHandler.postDelayed(cooldownTick, TICK_MS)
    }

    private val cooldownHandler = Handler(Looper.getMainLooper())
    private val cooldownTick = Runnable { renderVideoProgress() }
    private var screenResumed = false

    override fun onPause() {
        screenResumed = false
        cooldownHandler.removeCallbacks(cooldownTick)
        super.onPause()
    }

    // Làm tròn lên giây để không hiện 00:00 khi vẫn còn chờ vài trăm ms.
    private fun formatMmSs(ms: Long): String {
        val total = (ms + 999) / 1000
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
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
        private const val PACK_SPAN = 3
        // Placement rewarded trong app/assets/ads/*placements.json.
        private const val REWARDED_KEY = "rewarded_coin"
        private const val DONE_ALPHA = 0.5f
        private const val TICK_MS = 1000L
    }
}
