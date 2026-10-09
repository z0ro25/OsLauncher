package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab

// Nguồn dữ liệu theme. UI chỉ đọc qua ThemeRepositoryProvider nên đổi nguồn không phải sửa fragment.
interface ThemeRepository {
    suspend fun getCategories(): List<ThemeCategory>
    suspend fun getThemes(tab: ThemeTab, categoryId: String): List<ThemeItem>

    // Chi tiết 1 theme (nền + icon để áp dụng); null khi không lấy được.
    suspend fun getThemeDetail(id: String): ThemeDetail?

    // Danh sách wallpaper cho tab Wallpapers — lấy từ backgrounds của theme (mỗi theme 1 thẻ).
    suspend fun getWallpapers(tab: ThemeTab, categoryId: String): List<PreviewCard>
}

object ThemeRepositoryProvider {
    // Categories lấy từ API (qua SAG); tự rơi về dữ liệu mẫu khi lỗi. Context lấy từ CommonSdk
    // (khởi tạo ở BaseLauncherApplication) nên không cần truyền vào fragment.
    val repository: ThemeRepository by lazy { ApiThemeRepository() }
}
