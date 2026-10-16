package com.ezla.oslauncher.themes.features.rewards

import com.ezla.oslauncher.themes.Models.CheckinDay

// Số liệu tạm theo Figma tới khi có logic check-in thật (gói xu: CoinPacks).
object RewardsSample {
    fun days(): List<CheckinDay> = (1..7).map { CheckinDay(it, 5, it == 1) }
}
