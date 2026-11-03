package com.ezla.oslauncher.themes.features.rewards

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.Models.CoinPack
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.data.CoinPacks
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DailyCheckinStore
import com.ezla.oslauncher.themes.data.DailyRewardStore
import com.ezla.oslauncher.themes.databinding.ThemesViewRewardsContentBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import com.truongnt.ios.ioslite.common.config.AppAds
import kotlinx.coroutines.launch
import java.util.Locale

// Logic nội dung Rewards (check-in, xem video, gói xu) dùng chung cho RewardsActivity và tab Rewards.
// Gói xu = IAP thật (CoinPacks, billing ở :app). Xem video: mỗi lượt +1 xu, 5 lượt/ngày, cách nhau 5 phút.
// [owner] là vòng đời của view: Activity, hoặc viewLifecycleOwner của Fragment.
class RewardsController(
    private val activity: FragmentActivity,
    private val owner: LifecycleOwner,
    private val binding: ThemesViewRewardsContentBinding,
    private val callback: RewardsCallback = object : RewardsCallback {},
    // Số dư mới sau khi nhận xu (check-in / video) để nơi chứa cập nhật badge xu.
    private val onBalanceChanged: (Int) -> Unit = {}
) {

    // Đang xem ad / đang cộng xu: chặn bấm lặp.
    private var watching = false
    private var claimingCheckin = false
    private var checkinAdapter: CheckinAdapter? = null
    private var resumed = false

    private val packAdapter = CoinPackAdapter(CoinPacks.packs()) { buyPack(it) }
    private val pricesListener: () -> Unit = { packAdapter.submit(CoinPacks.packs()) }
    private val coinsListener: (Int, Int) -> Unit = { coins, balance ->
        if (alive()) {
            CoinToastDialog(activity, coins).show()
            onBalanceChanged(balance)
        }
    }

    private val cooldownHandler = Handler(Looper.getMainLooper())
    private val cooldownTick = Runnable { renderVideoProgress() }

    fun setup() {
        binding.tvVideoCoin.text = activity.getString(R.string.themes_coin_plus, DailyRewardStore.COINS_PER_VIEW)
        renderVideoProgress()
        AppAds.preloadRewarded(REWARDED_KEY)

        checkinAdapter = CheckinBinder.bindGrid(binding.checkinCard.rvCheckin) { day ->
            callback.onCheckinDayClick(activity, day)
            if (day.isToday && !day.claimed) claimCheckin()
        }

        binding.rvPacks.layoutManager = object : GridLayoutManager(activity, PACK_SPAN) {
            override fun canScrollVertically() = false
        }
        binding.rvPacks.addItemDecoration(GridSpacingDecoration(PACK_SPAN, dp(15)))
        binding.rvPacks.adapter = packAdapter
        binding.layoutTooltip.setOnClickListener { toggleTooltip() }
        binding.layoutWatchVideo.setOnClickListener {
            callback.onWatchVideoClick(activity)
            watchVideo()
        }
    }

    fun toggleTooltip() {
        binding.layoutTooltip.visibility =
            if (binding.layoutTooltip.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    // Màn/tab hiện lại: làm mới 7 ô check-in + lượt video, và xử lý giao dịch xu bị kẹt
    // (kẹt thì Play chặn mua lại). Gắn callback giá/xu ở đây vì tab ẩn thì nhường cho màn khác.
    fun onResume() {
        resumed = true
        CoinPacks.onPricesLoaded = pricesListener
        CoinPacks.onCoinsAdded = coinsListener
        renderVideoProgress()
        checkinAdapter?.submit(DailyCheckinStore.days(activity))
        CoinPacks.syncPending()
    }

    fun onPause() {
        resumed = false
        cooldownHandler.removeCallbacks(cooldownTick)
        // Chỉ gỡ khi callback còn là của nơi này (màn Rewards khác có thể đã gắn đè).
        if (CoinPacks.onPricesLoaded === pricesListener) CoinPacks.onPricesLoaded = null
        if (CoinPacks.onCoinsAdded === coinsListener) CoinPacks.onCoinsAdded = null
    }

    private fun alive() = !activity.isFinishing && !activity.isDestroyed

    // Bấm gói -> luồng mua của Play (billing ở :app); xu cộng xong báo về qua onCoinsAdded.
    private fun buyPack(pack: CoinPack) {
        callback.onPackClick(activity, pack)
        if (!CoinPacks.launchPurchase(activity, pack.id)) {
            Toast.makeText(activity, R.string.themes_store_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    // Bấm ô hôm nay: nhận xu check-in (cùng luồng nút Claim ở dialog), xong làm mờ ô.
    private fun claimCheckin() {
        if (claimingCheckin) return
        claimingCheckin = true
        owner.lifecycleScope.launch {
            val result = CheckinBinder.claimToday(activity)
            claimingCheckin = false
            if (result == null || !alive()) return@launch
            checkinAdapter?.submit(DailyCheckinStore.days(activity))
            onBalanceChanged(result.second)
            RewardDialog(activity, RewardDialog.Type.CHECKIN, result.first).show()
        }
    }

    // Còn nợ thưởng (lỗi mạng lần trước) thì bấm là cộng bù, không bắt xem lại.
    private fun watchVideo() {
        if (watching) return
        if (DailyRewardStore.isRewardPending(activity)) {
            claimPendingReward()
            return
        }
        if (DailyRewardStore.views(activity) >= DailyRewardStore.VIEWS_PER_DAY) {
            Toast.makeText(activity, R.string.themes_reward_done_today, Toast.LENGTH_SHORT).show()
            return
        }
        val left = DailyRewardStore.cooldownLeftMs(activity)
        if (left > 0L) {
            Toast.makeText(activity, activity.getString(R.string.themes_reward_wait_toast, formatMmSs(left)), Toast.LENGTH_SHORT).show()
            return
        }
        watching = true
        AppAds.showRewarded(activity, REWARDED_KEY) { earned ->
            watching = false
            if (!alive()) return@showRewarded
            if (!earned) {
                Toast.makeText(activity, R.string.themes_reward_no_ad, Toast.LENGTH_SHORT).show()
                return@showRewarded
            }
            DailyRewardStore.addView(activity)
            renderVideoProgress()
            claimPendingReward()
            AppAds.preloadRewarded(REWARDED_KEY)
        }
    }

    // Cộng toàn bộ lượt còn nợ trong 1 lần gọi; chỉ trừ nợ khi server OK, lỗi thì lần bấm sau cộng lại.
    private fun claimPendingReward() {
        val count = DailyRewardStore.pending(activity)
        if (count <= 0) return
        val coins = count * DailyRewardStore.COINS_PER_VIEW
        watching = true
        owner.lifecycleScope.launch {
            CreditRepository.addCredit(coins, DailyRewardStore.CREDIT_TYPE)
                .onSuccess { balance ->
                    DailyRewardStore.markClaimed(activity, count)
                    if (alive()) {
                        CoinToastDialog(activity, coins).show()
                        onBalanceChanged(balance)
                    }
                }
                .onFailure {
                    Toast.makeText(activity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                }
            watching = false
            renderVideoProgress()
        }
    }

    // Đang chờ -> "Next video in mm:ss" (đếm từng giây), không thì "Today x/5"; hết lượt thì làm mờ.
    private fun renderVideoProgress() {
        cooldownHandler.removeCallbacks(cooldownTick)
        val views = DailyRewardStore.views(activity)
        val left = DailyRewardStore.cooldownLeftMs(activity)
        val waiting = left > 0L && views < DailyRewardStore.VIEWS_PER_DAY && !DailyRewardStore.isRewardPending(activity)
        binding.tvVideoProgress.text = if (waiting) {
            activity.getString(R.string.themes_reward_cooldown, formatMmSs(left))
        } else {
            activity.getString(R.string.themes_reward_progress, views, DailyRewardStore.VIEWS_PER_DAY)
        }
        binding.layoutWatchVideo.alpha =
            if (waiting || DailyRewardStore.isDoneToday(activity)) DONE_ALPHA else 1f
        // Chỉ chạy đồng hồ khi đang hiện (dừng ở onPause), tránh tick nền vô ích.
        if (waiting && resumed) cooldownHandler.postDelayed(cooldownTick, TICK_MS)
    }

    // Làm tròn lên giây để không hiện 00:00 khi vẫn còn chờ vài trăm ms.
    private fun formatMmSs(ms: Long): String {
        val total = (ms + 999) / 1000
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()

    private companion object {
        const val PACK_SPAN = 3
        // Placement rewarded trong app/assets/ads/*placements.json.
        const val REWARDED_KEY = "rewarded_coin"
        const val DONE_ALPHA = 0.5f
        const val TICK_MS = 1000L
    }
}
