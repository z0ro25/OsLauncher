package com.ezla.oslauncher.themes.Models

import androidx.annotation.StringRes

// Model tạm theo UI Figma; khi có API sẽ map response vào đây (hoặc đổi theo schema thật).
// Dữ liệu mẫu dùng titleRes (localize được); dữ liệu từ API dùng title.
// iconUrl = thumbnail danh mục trên server; null (dữ liệu mẫu / server không có ảnh) -> chip chỉ có chữ.
data class ThemeCategory(
    val id: String,
    val iconUrl: String? = null,
    @StringRes val titleRes: Int = 0,
    val title: String = ""
)

// 1 trang wallpaper của chip All (loadmore); hasMore = server còn next_page_url.
data class WallpaperPage(val cards: List<PreviewCard>, val hasMore: Boolean)

data class ThemeItem(
    val id: String,
    val name: String,
    // URL (http, file:///android_asset/...) để Glide load; API sau này trả URL ảnh preview.
    val previewUrl: String,
    // is_unlocked của theme (phần icon); false -> hiện nhãn New.
    val isUnlocked: Boolean = false
)

enum class ThemeTab { FEATURED, TOP }

// Thẻ ở tab Icons: nền = ảnh background đầu của theme, icons = 6 logo đầu theo order.
data class IconThemeCard(
    val id: String,
    val name: String,
    val background: String?,
    val icons: List<String>,
    val isUnlocked: Boolean = false
)

// 1 section ở tab Icons = 1 danh mục + các theme của nó.
data class IconSection(
    val category: ThemeCategory,
    val themes: List<IconThemeCard>
)

// Loại nội dung của chip đầu tab Themes (Figma Main): mỗi loại 1 bộ section theo danh mục.
enum class HomeKind { THEME, ICON, WALLPAPER, CONTROL_CENTER }

// Thẻ Control Center: nền = ảnh background đầu của theme, phủ panel dựng tĩnh với skin của theme.
data class ControlCenterCard(
    val themeId: String,
    val name: String,
    val background: Any?,
    val skins: List<CcSkinItem>,
    val isDefault: Boolean = false,
    // Từ control_systems: giá mở khoá, màu "đang bật" (hex), ảnh nền panel (null = nền mặc định).
    val coins: Int = 0,
    val colors: List<String> = emptyList(),
    val panelBackground: String? = null
)

// 1 section ở tab Themes: [items] null = đang tải (hiện thẻ chờ), rỗng thì section bị gỡ.
data class HomeSection(
    val category: ThemeCategory,
    val items: List<Any>? = null
)
