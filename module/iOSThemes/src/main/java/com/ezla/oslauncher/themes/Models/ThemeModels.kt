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

// Thẻ ở tab Icons: nền = ảnh background đầu của theme, icons = 6 logo đầu theo order.
data class IconThemeCard(
    val id: String,
    val name: String,
    val background: String?,
    val icons: List<String>
)

// 1 section ở tab Icons = 1 danh mục + các theme của nó.
data class IconSection(
    val category: ThemeCategory,
    val themes: List<IconThemeCard>
)
