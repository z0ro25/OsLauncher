package com.ezla.oslauncher.themes.features.rewards

import com.ezla.oslauncher.themes.Models.CheckinDay
import com.ezla.oslauncher.themes.Models.CoinPack
import android.app.Activity

// Hành động màn Rewards — để sẵn, chưa nối (check-in, rewarded ads, mua gói xu).
interface RewardsCallback {
    fun onCheckinDayClick(activity: Activity, day: CheckinDay) {}
    fun onWatchVideoClick(activity: Activity) {}
    fun onPackClick(activity: Activity, pack: CoinPack) {}
}
