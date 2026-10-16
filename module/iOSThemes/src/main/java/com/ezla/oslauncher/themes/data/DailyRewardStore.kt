package com.ezla.oslauncher.themes.data

import android.content.Context
import com.truongnt.ios.ioslite.common.util.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Thưởng xem video hằng ngày: xem đủ VIEWS_PER_DAY rewarded thì +COINS xu (1 lần/ngày).
// Key prefs chính là ngày ddMMyyyy -> sang ngày mới tự reset, không cần so ngày hay dọn key cũ.
object DailyRewardStore {

    const val VIEWS_PER_DAY = 3
    const val COINS = 5
    const val CREDIT_TYPE = "view_ads"

    private const val KEY_VIEWS = "themes_reward_views_"
    private const val KEY_CLAIMED = "themes_reward_claimed_"

    fun views(context: Context): Int =
        PreferencesUtil.getInt(context, KEY_VIEWS + today(), 0).coerceAtMost(VIEWS_PER_DAY)

    fun isClaimed(context: Context): Boolean =
        PreferencesUtil.getBoolean(context, KEY_CLAIMED + today(), false)

    // Đủ lượt xem nhưng cộng xu lỗi (mạng) -> còn nợ thưởng, bấm lại để cộng, không bắt xem thêm.
    fun isRewardPending(context: Context) = views(context) >= VIEWS_PER_DAY && !isClaimed(context)

    fun canWatch(context: Context) = views(context) < VIEWS_PER_DAY

    // Ghi 1 lượt xem đủ (SDK báo onEarnedReward); trả số lượt hôm nay sau khi ghi.
    fun addView(context: Context): Int {
        val key = KEY_VIEWS + today()
        val next = (PreferencesUtil.getInt(context, key, 0) + 1).coerceAtMost(VIEWS_PER_DAY)
        PreferencesUtil.putInt(context, key, next)
        return next
    }

    // Chỉ ghi sau khi server cộng xu OK, để lỗi thì còn cộng lại được.
    fun markClaimed(context: Context) {
        PreferencesUtil.putBoolean(context, KEY_CLAIMED + today(), true)
    }

    private fun today(): String = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
}
