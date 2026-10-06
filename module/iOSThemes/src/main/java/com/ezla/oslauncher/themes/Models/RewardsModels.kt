package com.ezla.oslauncher.themes.Models

import androidx.annotation.DrawableRes

data class CheckinDay(val day: Int, val coins: Int, val isToday: Boolean)

data class CoinPack(val id: String, val coins: Int, val price: String, @DrawableRes val icon: Int)
