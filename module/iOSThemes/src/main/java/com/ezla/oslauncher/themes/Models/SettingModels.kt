package com.ezla.oslauncher.themes.Models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class SettingItem { RATE, CONTACT, UPDATE, SHARE, PRIVACY, SUBSCRIPTION }

data class SettingRow(
    val item: SettingItem,
    @DrawableRes val icon: Int,
    @StringRes val title: Int
)
