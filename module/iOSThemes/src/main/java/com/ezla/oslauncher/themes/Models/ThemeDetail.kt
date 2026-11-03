package com.ezla.oslauncher.themes.Models

// Chi tiết 1 theme để dựng màn Install + áp dụng: danh sách ảnh nền (wallpapers) và icon
// đè theo package (logos). Ảnh là URL http, tải lúc áp; logos ánh xạ package_name -> app trên máy.
data class ThemeDetail(
    val id: String,
    val name: String,
    val previewUrl: String,
    // Giá mở khoá phần icon / phần nền (số xu) từ API.
    val iconCoins: Int,
    val wallpaperCoins: Int,
    val wallpapers: List<String>,
    val logos: List<ThemeLogoItem>,
    // Mở khoá: icon theo id theme, nền theo id background đầu (0 = không có nền).
    // Trạng thái lấy từ is_unlocked server trả (lưu theo device).
    val backgroundId: Int = 0,
    val iconUnlocked: Boolean = false,
    val wallpaperUnlocked: Boolean = false,
    // Ảnh nền ô Control Center (control_systems.items); rỗng = theme không có skin Control Center.
    val controlSystems: List<CcSkinItem> = emptyList(),
    // Phần còn lại của control_systems: giá, màu "đang bật", ảnh nền panel; null = không có.
    val controlSkin: ThemeControlSkin? = null
)

// control_systems đã map: [coins] giá mở khoá, [colors] 1-3 mã hex màu "đang bật",
// [background] URL ảnh chỉ để xem trước (không áp vào panel), [isUnlocked] trạng thái mở khoá.
data class ThemeControlSkin(
    val items: List<CcSkinItem>,
    val coins: Int,
    val colors: List<String>,
    val background: String?,
    val isUnlocked: Boolean = false
)

// 1 icon của theme: [packageName] là mã app của server (app_xxx), ánh xạ sang app thật bằng
// AppCodeResolver; [label] = tên hiện dưới icon ở preview.
data class ThemeLogoItem(
    val packageName: String,
    val imageUrl: String,
    val label: String = ""
)

// 1 ảnh nền ô Control Center: [name] là loại ô của server (vd "cc bg 2x2"), [imageUrl] ảnh full.
data class CcSkinItem(
    val name: String,
    val imageUrl: String
)
