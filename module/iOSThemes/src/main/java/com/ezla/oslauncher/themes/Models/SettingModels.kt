package com.ezla.oslauncher.themes.Models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class SettingItem { LANGUAGE, RATE, CONTACT, UPDATE, SHARE, PRIVACY, SUBSCRIPTION }

data class SettingRow(
    val item: SettingItem,
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    // Chữ phụ bên phải (vd ngôn ngữ đang dùng); null = không hiện.
    val value: String? = null
)
