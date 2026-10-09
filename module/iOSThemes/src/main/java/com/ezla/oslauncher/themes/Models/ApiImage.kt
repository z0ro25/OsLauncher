package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Ảnh từ API, categories và themes dùng chung: disk + path + size + URL theo từng kích cỡ.
data class ApiImage(
    @SerializedName("disk") val disk: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("size") val size: ImageSize? = null,
    @SerializedName("url") val url: ImageUrl? = null
)

data class ImageSize(
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class ImageUrl(
    @SerializedName("full") val full: String? = null,
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("small") val small: String? = null,
    @SerializedName("extra_small") val extraSmall: String? = null
)
