package com.ezla.oslauncher.themes.features.rewards

import android.content.Context
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.Models.CheckinDay
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DailyCheckinStore

// Dùng chung màn Rewards + CheckinDialog: dựng lưới 7 ô (4 cột, Day 7 chiếm 2) và nhận xu hôm nay.
object CheckinBinder {

    private const val SPAN = 4
    private const val GAP_DP = 8

    fun bindGrid(rv: RecyclerView, onClick: (CheckinDay) -> Unit): CheckinAdapter {
        val ctx = rv.context
        val adapter = CheckinAdapter(DailyCheckinStore.days(ctx), onClick)
        rv.layoutManager = object : GridLayoutManager(ctx, SPAN) {
            override fun canScrollVertically() = false
        }.apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == DailyCheckinStore.DAYS - 1) 2 else 1
            }
        }
        rv.addItemDecoration(SpanAwareSpacingDecoration((GAP_DP * ctx.resources.displayMetrics.density).toInt()))
        rv.adapter = adapter
        return adapter
    }

    // Nhận xu Day hôm nay qua POST /credits; chỉ đánh dấu đã nhận khi server OK.
    // Trả (xu vừa cộng, số dư mới); đã nhận rồi hoặc lỗi (có toast) -> null.
    suspend fun claimToday(context: Context): Pair<Int, Int>? {
        if (DailyCheckinStore.isClaimedToday(context)) return null
        val day = DailyCheckinStore.todayDay(context)
        val coins = DailyCheckinStore.coinsFor(day)
        return CreditRepository.addCredit(coins, DailyCheckinStore.CREDIT_TYPE).fold(
            onSuccess = { balance ->
                DailyCheckinStore.markClaimed(context, day)
                coins to balance
            },
            onFailure = {
                Toast.makeText(context, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                null
            }
        )
    }
}
