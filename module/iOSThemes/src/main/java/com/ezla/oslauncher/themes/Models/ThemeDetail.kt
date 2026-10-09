package com.ezla.oslauncher.themes.Models

// Chi tiết 1 theme để dựng màn Install + áp dụng: danh sách ảnh nền (wallpapers) và icon
// đè theo package (logos). Ảnh là URL http, tải lúc áp; logos ánh xạ package_name -> app trên máy.
data class ThemeDetail(
    val id: String,
    val name: String,
    val previewUrl: String,
    val coins: Int,
    val wallpapers: List<String>,
    val logos: List<ThemeLogoItem>
)

// 1 icon của theme: áp cho app có [packageName], ảnh ở [imageUrl].
data class ThemeLogoItem(
    val packageName: String,
    val imageUrl: String
)
