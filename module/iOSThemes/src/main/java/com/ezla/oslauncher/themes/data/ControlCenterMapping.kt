package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.ApiTheme
import com.ezla.oslauncher.themes.Models.CcSkinItem
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.ThemeControlSkin

// Map control_systems của theme API -> ảnh skin Control Center, dùng chung cho tab Themes và Mine.
// Bỏ ảnh tắt (active = 0) và ảnh không có URL; lấy ảnh full vì ô phải nét khi phóng to.
internal fun ApiTheme.ccSkins(): List<CcSkinItem> = controlSystems?.items.orEmpty()
    .filter { it.active != 0 }
    .mapNotNull { cs ->
        val url = cs.image.firstOrNull()?.url
        val img = url?.full ?: url?.medium ?: return@mapNotNull null
        CcSkinItem(cs.name.orEmpty(), img)
    }

// Toàn bộ control_systems (ảnh ô + giá + màu + ảnh nền panel); null khi theme không có skin.
internal fun ApiTheme.ccSkin(): ThemeControlSkin? {
    val cs = controlSystems ?: return null
    val items = ccSkins()
    val bg = cs.background.firstOrNull()?.url
    val background = bg?.full ?: bg?.medium
    if (items.isEmpty() && cs.color.isEmpty() && background == null) return null
    return ThemeControlSkin(
        items = items, coins = cs.coins, colors = cs.color, background = background, isUnlocked = cs.isUnlocked
    )
}

// null khi theme không có skin Control Center (không hiện thẻ).
internal fun ApiTheme.toControlCenterCard(): ControlCenterCard? {
    val skin = ccSkin() ?: return null
    val bg = backgrounds.flatMap { it.image }.firstOrNull()?.url
    return ControlCenterCard(
        themeId = id.toString(), name = name, background = bg?.medium ?: bg?.full,
        skins = skin.items, coins = skin.coins, colors = skin.colors, panelBackground = skin.background
    )
}
