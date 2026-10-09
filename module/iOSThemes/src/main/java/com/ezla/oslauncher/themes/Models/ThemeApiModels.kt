package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Response GET /api/v1/themes (phân trang kiểu Laravel; next/prev có thể null).
data class ThemePage(
    @SerializedName("current_page") val currentPage: Int = 0,
    @SerializedName("current_page_url") val currentPageUrl: String? = null,
    @SerializedName("data") val items: List<ApiTheme> = emptyList(),
    @SerializedName("first_page_url") val firstPageUrl: String? = null,
    @SerializedName("from") val from: Int? = null,
    @SerializedName("next_page_url") val nextPageUrl: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("per_page") val perPage: Int = 0,
    @SerializedName("prev_page_url") val prevPageUrl: String? = null,
    @SerializedName("to") val to: Int? = null
)

data class ApiTheme(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("category_id") val categoryId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("thumbnail") val thumbnail: List<ApiImage> = emptyList(),
    @SerializedName("order") val order: Int = 0,
    // Giá mở theme (số xu) do server trả; 0 = chưa có server cũ.
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("backgrounds") val backgrounds: List<ThemeBackground> = emptyList(),
    @SerializedName("logos") val logos: List<ThemeLogo> = emptyList()
)

data class ThemeBackground(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("theme_id") val themeId: Int = 0,
    @SerializedName("image") val image: List<ApiImage> = emptyList(),
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("order") val order: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class ThemeLogo(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("theme_id") val themeId: Int = 0,
    @SerializedName("label") val label: String? = null,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("image") val image: List<ApiImage> = emptyList(),
    @SerializedName("order") val order: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
