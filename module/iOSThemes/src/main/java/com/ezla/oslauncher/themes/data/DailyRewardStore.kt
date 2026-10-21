package com.ezla.oslauncher.themes.data

import android.content.Context
import com.truongnt.ios.ioslite.common.util.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Thưởng xem video: tối đa VIEWS_PER_DAY lượt/ngày, mỗi lượt +COINS_PER_VIEW xu, 2 lượt cách nhau
// COOLDOWN_MS (tính từ lúc được thưởng). Key lượt xem/nợ theo ngày ddMMyyyy -> sang ngày tự reset.
object DailyRewardStore {

    const val VIEWS_PER_DAY = 5
    const val COINS_PER_VIEW = 1
    const val COOLDOWN_MS = 5 * 60_000L
    const val CREDIT_TYPE = "view_ads"

    private const val KEY_VIEWS = "themes_reward_views_"
    // Số lượt đã xem nhưng cộng xu lỗi (mạng): bấm lại thì cộng bù, không bắt xem lại.
    private const val KEY_PENDING = "themes_reward_pending_"
    // Mốc được thưởng gần nhất (ms); không theo ngày để chờ 5 phút vẫn đúng lúc qua 00:00.
    private const val KEY_LAST_REWARD_AT = "themes_reward_last_at"

    fun views(context: Context): Int =
        PreferencesUtil.getInt(context, KEY_VIEWS + today(), 0).coerceAtMost(VIEWS_PER_DAY)

    fun pending(context: Context): Int =
        PreferencesUtil.getInt(context, KEY_PENDING + today(), 0).coerceAtLeast(0)

    fun isRewardPending(context: Context) = pending(context) > 0

    // Đã dùng hết lượt hôm nay và không còn nợ thưởng.
    fun isDoneToday(context: Context) = views(context) >= VIEWS_PER_DAY && !isRewardPending(context)

    fun canWatch(context: Context) = views(context) < VIEWS_PER_DAY && cooldownLeftMs(context) == 0L

    fun cooldownLeftMs(context: Context): Long {
        val last = PreferencesUtil.getLong(context, KEY_LAST_REWARD_AT, 0L)
        if (last <= 0L) return 0L
        val elapsed = System.currentTimeMillis() - last
        // Đồng hồ máy bị chỉnh lùi (elapsed âm) -> không khoá người dùng.
        return if (elapsed < 0) 0L else (COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    // Ghi 1 lượt xem đủ (SDK báo onEarnedReward): +1 lượt, +1 nợ thưởng, bắt đầu tính 5 phút.
    fun addView(context: Context) {
        val day = today()
        val next = (PreferencesUtil.getInt(context, KEY_VIEWS + day, 0) + 1).coerceAtMost(VIEWS_PER_DAY)
        PreferencesUtil.putInt(context, KEY_VIEWS + day, next)
        PreferencesUtil.putInt(context, KEY_PENDING + day, pending(context) + 1)
        PreferencesUtil.putLong(context, KEY_LAST_REWARD_AT, System.currentTimeMillis())
    }

    // Chỉ gọi sau khi server cộng xu OK cho [count] lượt nợ.
    fun markClaimed(context: Context, count: Int) {
        PreferencesUtil.putInt(context, KEY_PENDING + today(), (pending(context) - count).coerceAtLeast(0))
    }

    private fun today(): String = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
}
