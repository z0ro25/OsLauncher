package com.ezla.oslauncher.themes.Models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

// Model tạm theo UI Figma; khi có API sẽ map response vào đây (hoặc đổi theo schema thật).
// Dữ liệu mẫu dùng titleRes (localize được); dữ liệu từ API dùng title.
data class ThemeCategory(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int = 0,
    val title: String = ""
)

data class ThemeItem(
    val id: String,
    val name: String,
    // URL (http, file:///android_asset/...) để Glide load; API sau này trả URL ảnh preview.
    val previewUrl: String
)

enum class ThemeTab { FEATURED, TOP }
