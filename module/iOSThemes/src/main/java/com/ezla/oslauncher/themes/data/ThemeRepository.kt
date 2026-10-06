package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab

// Nguồn dữ liệu theme. Hiện dùng SampleThemeRepository; khi có API chỉ cần thêm bản
// triển khai mới và đổi ThemeRepositoryProvider, UI không phải sửa.
interface ThemeRepository {
    suspend fun getCategories(): List<ThemeCategory>
    suspend fun getThemes(tab: ThemeTab, categoryId: String): List<ThemeItem>
}

object ThemeRepositoryProvider {
    val repository: ThemeRepository by lazy { SampleThemeRepository() }
}
