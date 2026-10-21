package com.ezla.oslauncher.themes.data

import android.content.Context
import com.ezla.oslauncher.themes.Models.CheckinDay
import com.truongnt.ios.ioslite.common.util.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Check-in hằng ngày (7 ngày). Chỉ tính khi đã nhận; nhận liên tiếp thì lên Day kế, sau Day 7 về
// Day 1; lỡ 1 ngày (hôm qua không nhận) cũng về Day 1. Lưu ngày nhận gần nhất + Day đã nhận.
object DailyCheckinStore {

    const val DAYS = 7
    const val CREDIT_TYPE = "daily_checkin"

    private const val KEY_LAST_DATE = "themes_checkin_last_date"
    private const val KEY_LAST_DAY = "themes_checkin_last_day"
    // Key = ngày ddMMyyyy: dialog chỉ tự hiện 1 lần/ngày.
    private const val KEY_DIALOG_SHOWN = "themes_checkin_dialog_"

    // Day 1-2: 2 xu, Day 3-5: 3 xu, Day 6-7: 5 xu.
    fun coinsFor(day: Int): Int = when (day) {
        in 1..2 -> 2
        in 3..5 -> 3
        else -> 5
    }

    fun isClaimedToday(context: Context): Boolean =
        PreferencesUtil.getString(context, KEY_LAST_DATE, "") == dateKey(0)

    // Day của hôm nay: đã nhận hôm nay -> chính Day đó; nhận hôm qua -> Day kế; còn lại -> Day 1.
    fun todayDay(context: Context): Int {
        val lastDate = PreferencesUtil.getString(context, KEY_LAST_DATE, "")
        val lastDay = PreferencesUtil.getInt(context, KEY_LAST_DAY, 0).coerceIn(0, DAYS)
        return when (lastDate) {
            dateKey(0) -> lastDay.coerceAtLeast(1)
            dateKey(-1) -> lastDay % DAYS + 1
            else -> 1
        }
    }

    // Lưới 7 ô: ô trước hôm nay = đã nhận; ô hôm nay đã nhận nếu isClaimedToday.
    fun days(context: Context): List<CheckinDay> {
        val today = todayDay(context)
        val claimedToday = isClaimedToday(context)
        return (1..DAYS).map { day ->
            CheckinDay(
                day = day,
                coins = coinsFor(day),
                isToday = day == today,
                claimed = day < today || (day == today && claimedToday)
            )
        }
    }

    // Gọi sau khi server cộng xu OK (lỗi thì chưa ghi để nhận lại được).
    fun markClaimed(context: Context, day: Int) {
        PreferencesUtil.putString(context, KEY_LAST_DATE, dateKey(0))
        PreferencesUtil.putInt(context, KEY_LAST_DAY, day)
    }

    // Lần mở Themes đầu tiên trong ngày và chưa nhận -> nên hiện dialog.
    fun shouldShowDialog(context: Context): Boolean =
        !isClaimedToday(context) && !PreferencesUtil.getBoolean(context, KEY_DIALOG_SHOWN + dateKey(0), false)

    // Ghi SAU khi dialog đã hiện được, để lỗi hiện không làm mất lượt hôm nay.
    fun markDialogShown(context: Context) {
        PreferencesUtil.putBoolean(context, KEY_DIALOG_SHOWN + dateKey(0), true)
    }

    // ddMMyyyy của hôm nay (+offset ngày) theo giờ máy.
    private fun dateKey(offsetDays: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
        return SimpleDateFormat("ddMMyyyy", Locale.US).format(cal.time)
    }
}
