package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.IconPack

// Dữ liệu màn Mine. "Owned" = đồ device đã mở khoá (theme/nền lấy từ API);
// theme mặc định do MineFragment tự thêm lên đầu qua DefaultThemeProvider.
interface MineRepository {
    suspend fun getOwnedThemes(): List<PreviewCard>
    suspend fun getOwnedWallpapers(): List<PreviewCard>
    suspend fun getOwnedIconPacks(): List<IconPack>
    suspend fun getFavorites(): List<IconPack>

    suspend fun getSuggestThemes(): List<PreviewCard>
    suspend fun getSuggestWallpapers(): List<PreviewCard>
    suspend fun getSuggestIconPacks(): List<IconPack>

    // Tab Control Center: theme đã mở khoá / gợi ý có ảnh control_systems.
    suspend fun getOwnedControlCenters(): List<ControlCenterCard> = emptyList()
    suspend fun getSuggestControlCenters(): List<ControlCenterCard> = emptyList()
}

object MineRepositoryProvider {
    val repository: MineRepository by lazy { ApiMineRepository() }
}
