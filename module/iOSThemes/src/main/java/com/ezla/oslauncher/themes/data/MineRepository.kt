package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.IconPack

// Dữ liệu màn Mine. "Owned" = đồ người dùng đã tải/lưu (chưa có nguồn nên rỗng);
// theme mặc định do MineFragment tự thêm lên đầu qua DefaultThemeProvider.
interface MineRepository {
    suspend fun getOwnedThemes(): List<PreviewCard>
    suspend fun getOwnedWallpapers(): List<PreviewCard>
    suspend fun getOwnedIconPacks(): List<IconPack>
    suspend fun getFavorites(): List<IconPack>

    suspend fun getSuggestThemes(): List<PreviewCard>
    suspend fun getSuggestWallpapers(): List<PreviewCard>
    suspend fun getSuggestIconPacks(): List<IconPack>
}

object MineRepositoryProvider {
    val repository: MineRepository by lazy { SampleMineRepository() }
}
