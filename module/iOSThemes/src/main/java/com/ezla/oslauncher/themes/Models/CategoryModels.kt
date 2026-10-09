package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Response GET /api/v1/categories: { "data": { current_page, data:[...], per_page, next/prev_page_url } }.
data class CategoryPage(
    @SerializedName("current_page") val currentPage: Int = 0,
    @SerializedName("data") val items: List<Category> = emptyList(),
    @SerializedName("per_page") val perPage: Int = 0,
    @SerializedName("next_page_url") val nextPageUrl: String? = null,
    @SerializedName("prev_page_url") val prevPageUrl: String? = null
)

data class Category(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("thumbnail") val thumbnail: List<ApiImage> = emptyList(),
    @SerializedName("active") val active: Int = 0,
    @SerializedName("order") val order: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
