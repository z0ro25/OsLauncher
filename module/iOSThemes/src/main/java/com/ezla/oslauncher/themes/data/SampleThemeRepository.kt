package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.IconSection
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.Models.WallpaperPage
import com.ezla.oslauncher.themes.R

// Dữ liệu mẫu lấy từ Figma (ảnh ở assets/themes_sample) để dựng UI trước khi có API.
class SampleThemeRepository : ThemeRepository {

    override suspend fun getCategories(): List<ThemeCategory> = listOf(
        ThemeCategory("new", titleRes = R.string.themes_category_new),
        ThemeCategory("os27", titleRes = R.string.themes_category_os27),
        ThemeCategory("black", titleRes = R.string.themes_category_black),
        ThemeCategory("cartoon", titleRes = R.string.themes_category_cartoon),
        ThemeCategory("neon", titleRes = R.string.themes_category_neon),
        ThemeCategory("cute", titleRes = R.string.themes_category_cute),
        ThemeCategory("sport", titleRes = R.string.themes_category_sport)
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

    // Mẫu không có nền/icon thật -> chi tiết rỗng để màn Install vẫn mở được khi API lỗi.
    override suspend fun getThemeDetail(id: String): ThemeDetail? =
        ThemeDetail(id = id, name = id, previewUrl = asset("snow.jpg"), iconCoins = 3, wallpaperCoins = 3, wallpapers = emptyList(), logos = emptyList())

    // Tab Wallpapers: dùng chính ảnh mẫu của theme làm thẻ wallpaper.
    override suspend fun getWallpapers(tab: ThemeTab, categoryId: String): List<PreviewCard> =
        getThemes(tab, categoryId).map { PreviewCard(it.id, it.name, it.previewUrl) }

    // Mẫu chỉ có 1 trang.
    override suspend fun getAllWallpapers(page: Int): WallpaperPage? =
        WallpaperPage(if (page == 1) getWallpapers(ThemeTab.FEATURED, "") else emptyList(), hasMore = false)

    // Mẫu không có icon theo theme -> mỗi danh mục 1 section, thẻ chỉ có nền là ảnh mẫu.
    override suspend fun getIconSections(categories: List<ThemeCategory>): List<IconSection> =
        categories.map { category ->
            IconSection(category, getThemes(ThemeTab.FEATURED, category.id).map {
                IconThemeCard(it.id, it.name, it.previewUrl, emptyList())
            })
        }

    private fun asset(name: String) = "file:///android_asset/themes_sample/$name"
}
