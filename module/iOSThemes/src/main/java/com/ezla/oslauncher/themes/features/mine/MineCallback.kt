package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.MineTab


// Hành động ở màn Mine — để sẵn, chưa nối (áp theme/wallpaper/icon, mở danh sách "more").
// card.isDefault / pack.isDefault = true là theme mặc định có sẵn của launcher.
interface MineCallback {
    fun onThemeClick(card: PreviewCard) {}
    fun onWallpaperClick(card: PreviewCard) {}
    fun onIconPackClick(pack: IconPack, fromFavorites: Boolean) {}
    fun onMoreClick(tab: MineTab, isLibrary: Boolean) {}
}
