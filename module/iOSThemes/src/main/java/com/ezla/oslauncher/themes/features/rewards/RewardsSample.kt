package com.ezla.oslauncher.themes.features.rewards

import com.ezla.oslauncher.themes.Models.CheckinDay
import com.ezla.oslauncher.themes.Models.CoinPack
import com.ezla.oslauncher.themes.R

// Số liệu tạm theo Figma tới khi có logic xu/IAP thật.
object RewardsSample {
    const val VIDEO_COINS = 45

    fun days(): List<CheckinDay> = (1..7).map { CheckinDay(it, 5, it == 1) }

    fun packs(): List<CoinPack> = listOf(
        CoinPack("coins_500", 500, "$1.99", R.drawable.themes_ic_coins_500),
        CoinPack("coins_1500", 1500, "$2.99", R.drawable.themes_ic_coins_1500),
        CoinPack("coins_2500", 2500, "$3.99", R.drawable.themes_ic_coins_2500),
        CoinPack("coins_3500", 3500, "$4.99", R.drawable.themes_ic_coins_3500),
        CoinPack("coins_5000", 5000, "$5.99", R.drawable.themes_ic_coins_5000),
        CoinPack("coins_6000", 6000, "$6.99", R.drawable.themes_ic_coins_6000)
    )
}
