package com.ezla.oslauncher.themes.Models

// image là kiểu Glide nhận được: @DrawableRes Int (mặc định có sẵn) hoặc String URL (API).
data class PreviewCard(
    val id: String,
    val name: String,
    val image: Any,
    val isDefault: Boolean = false
)

// background = ảnh nền thẻ (theme mặc định dùng wallpaper mặc định); null = nền trắng như Figma.
data class IconPack(
    val id: String,
    val name: String,
    val icons: List<Any>,
    val isDefault: Boolean = false,
    val background: Any? = null
)

enum class MineTab { THEME, WALLPAPERS, ICON, FAVORITES }
