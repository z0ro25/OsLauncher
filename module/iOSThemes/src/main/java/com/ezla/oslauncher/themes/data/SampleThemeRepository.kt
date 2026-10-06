package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R

// Dữ liệu mẫu lấy từ Figma (ảnh ở assets/themes_sample) để dựng UI trước khi có API.
class SampleThemeRepository : ThemeRepository {

    override suspend fun getCategories(): List<ThemeCategory> = listOf(
        ThemeCategory("new", R.drawable.themes_ic_chip_new, R.string.themes_category_new),
        ThemeCategory("os27", R.drawable.themes_ic_chip_os27, R.string.themes_category_os27),
        ThemeCategory("black", R.drawable.themes_ic_chip_black, R.string.themes_category_black),
        ThemeCategory("cartoon", R.drawable.themes_ic_chip_cartoon, R.string.themes_category_cartoon),
        ThemeCategory("neon", R.drawable.themes_ic_chip_neon, R.string.themes_category_neon),
        ThemeCategory("cute", R.drawable.themes_ic_chip_cute, R.string.themes_category_cute),
        ThemeCategory("sport", R.drawable.themes_ic_chip_sport, R.string.themes_category_sport)
    )

    override suspend fun getThemes(tab: ThemeTab, categoryId: String): List<ThemeItem> {
        val base = listOf(
            ThemeItem("snow", "Snow", asset("snow.jpg")),
            ThemeItem("halloween", "Halloween", asset("halloween.jpg")),
            ThemeItem("cute", "Cute", asset("cute.jpg")),
            ThemeItem("matcha_glass", "Matcha Glass", asset("matcha_glass.jpg"))
        )
        return if (tab == ThemeTab.TOP) base.reversed() else base
    }

    private fun asset(name: String) = "file:///android_asset/themes_sample/$name"
}
