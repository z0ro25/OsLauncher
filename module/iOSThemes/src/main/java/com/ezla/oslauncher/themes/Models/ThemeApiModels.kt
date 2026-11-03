package com.ezla.oslauncher.themes.Models

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

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

// Response GET /api/v1/my-backgrounds: phân trang như ThemePage, item là ThemeBackground.
data class BackgroundPage(
    @SerializedName("current_page") val currentPage: Int = 0,
    @SerializedName("data") val items: List<ThemeBackground> = emptyList(),
    @SerializedName("next_page_url") val nextPageUrl: String? = null,
    @SerializedName("per_page") val perPage: Int = 0,
    @SerializedName("prev_page_url") val prevPageUrl: String? = null
)

data class ApiTheme(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("category_id") val categoryId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("thumbnail") val thumbnail: List<ApiImage> = emptyList(),
    // Giá mở khoá phần icon của theme (số xu).
    @SerializedName("icons_coins") val iconsCoins: Int = 0,
    @SerializedName("order") val order: Int = 0,
    // User đã mở khoá phần icon chưa (server lưu theo device).
    @SerializedName("is_unlocked") val isUnlocked: Boolean = false,
    @SerializedName("backgrounds") val backgrounds: List<ThemeBackground> = emptyList(),
    @SerializedName("logos") val logos: List<ThemeLogo> = emptyList(),
    // Skin Control Center của theme; chỉ có khi `fields` có "control_systems". null = không có.
    @JsonAdapter(ThemeControlSystemsAdapter::class)
    @SerializedName("control_systems") val controlSystems: ThemeControlSystems? = null
)

// control_systems: giá mở khoá, màu "đang bật" (1-3 mã hex), ảnh nền panel và ảnh nền từng ô.
data class ThemeControlSystems(
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("color") val color: List<String> = emptyList(),
    @SerializedName("background") val background: List<ApiImage> = emptyList(),
    @SerializedName("items") val items: List<ThemeControlSystem> = emptyList(),
    // Device đã mở khoá Control Center của theme chưa; server chưa trả thì mặc định false (còn khoá).
    @SerializedName("is_unlocked") val isUnlocked: Boolean = false
)

// Server từng trả control_systems dạng mảng ô (bản cũ): đọc được cả 2 dạng để 1 theme lệch format
// không làm hỏng cả trang /themes. Mảng -> chỉ có items.
class ThemeControlSystemsAdapter : TypeAdapter<ThemeControlSystems>() {
    private val gson = Gson()

    override fun write(out: JsonWriter, value: ThemeControlSystems?) {
        gson.toJson(value, ThemeControlSystems::class.java, out)
    }

    override fun read(reader: JsonReader): ThemeControlSystems? = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> gson.fromJson<ThemeControlSystems>(reader, ThemeControlSystems::class.java)
        JsonToken.BEGIN_ARRAY -> {
            val type = object : TypeToken<List<ThemeControlSystem>>() {}.type
            ThemeControlSystems(items = gson.fromJson<List<ThemeControlSystem>>(reader, type).orEmpty())
        }
        else -> {
            reader.skipValue()
            null
        }
    }
}

data class ThemeBackground(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("theme_id") val themeId: Int = 0,
    @SerializedName("image") val image: List<ApiImage> = emptyList(),
    @SerializedName("active") val active: Int = 0,
    // Giá mở khoá nền này (số xu).
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("is_unlocked") val isUnlocked: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

// package_name là MÃ loại app của server (vd "app_photos"), không phải package Android.
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

// 1 ô trong control_systems.items: name là loại ô (vd "cc bg 1x1", "cc bg music"), server chưa có mã riêng.
data class ThemeControlSystem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("theme_id") val themeId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("image") val image: List<ApiImage> = emptyList(),
    @SerializedName("active") val active: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
