package com.ezla.oslauncher.themes.data.favorite

import androidx.room.Entity

// 1 dòng = 1 nền hoặc 1 bộ icon đã thả tim; lưu đủ để vẽ thẻ ở Mine > Favorites mà không gọi API.
// Item mặc định (itemId "default") không lưu ảnh: đọc lại từ DefaultThemeProvider vì id drawable đổi theo build.
@Entity(tableName = "themes_favorite", primaryKeys = ["type", "itemId"])
data class FavoriteEntity(
    val type: String,
    // Nền: backgroundId (0 thì dùng URL ảnh); bộ icon: id theme.
    val itemId: String,
    val name: String,
    // Nền: URL ảnh; bộ icon: ảnh nền thẻ.
    val image: String?,
    // URL icon của thẻ bộ icon, nối bằng '\n' (URL không chứa xuống dòng).
    val icons: String = "",
    val backgroundId: Int = 0,
    val coins: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
