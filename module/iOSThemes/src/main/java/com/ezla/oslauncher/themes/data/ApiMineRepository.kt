package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Api.ApiClient
import com.ezla.oslauncher.themes.Models.ApiTheme
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.truongnt.ios.ioslite.common.CommonSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Màn Mine: "owned" theme/nền lấy từ my-themes / my-backgrounds (đồ device đã mở khoá, qua SAG).
// Lỗi mạng/HTTP -> danh sách rỗng (chỉ còn thẻ mặc định). Suggest lấy /themes; Favorites còn mẫu.
class ApiMineRepository : MineRepository by SampleMineRepository() {

    override suspend fun getOwnedThemes(): List<PreviewCard> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext emptyList()
        runCatching {
            val response = ApiClient.api(ctx).getMyThemes(THEME_FIELDS, WHERE, ORDER_BY)
            if (!response.isSuccessful) error("my-themes failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty().map { theme ->
                val url = theme.thumbnail.firstOrNull()?.url
                PreviewCard(
                    id = theme.id.toString(), name = theme.name,
                    image = url?.medium ?: url?.full ?: url?.small ?: url?.extraSmall.orEmpty()
                )
            }
        }.getOrElse {
            emptyList()
        }
    }

    // Nền đã mở khoá: isUnlocked = true để bấm vào áp luôn, không hỏi mở khoá lại.
    override suspend fun getOwnedWallpapers(): List<PreviewCard> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext emptyList()
        runCatching {
            val response = ApiClient.api(ctx).getMyBackgrounds(BACKGROUND_FIELDS, WHERE, ORDER_BY)
            if (!response.isSuccessful) error("my-backgrounds failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty().mapNotNull { bg ->
                val url = bg.image.firstOrNull()?.url
                val image = url?.full ?: url?.medium ?: return@mapNotNull null
                PreviewCard(
                    id = bg.id.toString(), name = bg.name.orEmpty(), image = image,
                    backgroundId = bg.id, coins = bg.coins, isUnlocked = true
                )
            }
        }.getOrElse {
            emptyList()
        }
    }

    // Tab Icon: bộ icon đã mở = theme trong my-themes có is_unlocked (phần icon). Thẻ = nền theme
    // (background đầu, không có thì thumbnail) + 6 logo đầu; id = id theme để mở màn Icon detail.
    override suspend fun getOwnedIconPacks(): List<IconPack> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext emptyList()
        runCatching {
            val response = ApiClient.api(ctx).getMyThemes(ICON_FIELDS, WHERE, ORDER_BY)
            if (!response.isSuccessful) error("my-themes (icons) failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty()
                .filter { it.isUnlocked && it.logos.isNotEmpty() }
                .map { theme ->
                    val bg = theme.backgrounds.flatMap { it.image }.firstOrNull()?.url
                    val thumb = theme.thumbnail.firstOrNull()?.url
                    IconPack(
                        id = theme.id.toString(),
                        name = theme.name,
                        icons = theme.logos.take(PACK_ICONS).mapNotNull { logo ->
                            val url = logo.image.firstOrNull()?.url
                            url?.medium ?: url?.full
                        },
                        background = bg?.medium ?: bg?.full ?: thumb?.medium ?: thumb?.full
                    )
                }
        }.getOrElse {
            emptyList()
        }
    }

    // Suggest (tab Mine + View theme): /themes không lọc danh mục, bỏ đồ device đã mở khoá.
    // Theme/Icon bỏ theme có is_unlocked (phần icon); Wallpaper bỏ nền có is_unlocked.
    override suspend fun getSuggestThemes(): List<PreviewCard> =
        suggestThemes(SUGGEST_THEME_FIELDS, "themes") { theme ->
            if (theme.isUnlocked) return@suggestThemes null
            val url = theme.thumbnail.firstOrNull()?.url
            val image = url?.medium ?: url?.full ?: url?.small ?: return@suggestThemes null
            PreviewCard(id = theme.id.toString(), name = theme.name, image = image)
        }

    override suspend fun getSuggestWallpapers(): List<PreviewCard> =
        suggestThemes(SUGGEST_WALLPAPER_FIELDS, "wallpapers") { theme ->
            val bg = theme.backgrounds.firstOrNull() ?: return@suggestThemes null
            if (bg.isUnlocked) return@suggestThemes null
            val url = bg.image.firstOrNull()?.url
            val image = url?.full ?: url?.medium ?: return@suggestThemes null
            PreviewCard(
                id = theme.id.toString(), name = theme.name, image = image,
                backgroundId = bg.id, coins = bg.coins, isUnlocked = false
            )
        }

    override suspend fun getSuggestIconPacks(): List<IconPack> =
        suggestThemes(SUGGEST_ICON_FIELDS, "icons") { theme ->
            if (theme.isUnlocked || theme.logos.isEmpty()) return@suggestThemes null
            val bg = theme.backgrounds.flatMap { it.image }.firstOrNull()?.url
            val thumb = theme.thumbnail.firstOrNull()?.url
            IconPack(
                id = theme.id.toString(),
                name = theme.name,
                icons = theme.logos.sortedBy { it.order }.take(PACK_ICONS).mapNotNull { logo ->
                    val url = logo.image.firstOrNull()?.url
                    url?.medium ?: url?.full
                },
                background = bg?.medium ?: bg?.full ?: thumb?.medium ?: thumb?.full
            )
        }


    // Gọi /themes (mọi danh mục) rồi map từng theme; lỗi -> rỗng.
    private suspend fun <T> suggestThemes(
        fields: String,
        tag: String,
        map: (ApiTheme) -> T?
    ): List<T> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext emptyList()
        runCatching {
            val response = ApiClient.api(ctx).getThemes(fields, WHERE, SUGGEST_ORDER_BY)
            if (!response.isSuccessful) error("suggest $tag failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty().mapNotNull(map)
        }.getOrElse {
            emptyList()
        }
    }

    private companion object {
        // Theo curl của server; WHERE rỗng = lấy tất cả (không lọc theo category/theme).
        const val THEME_FIELDS = "id,category_id,name,thumbnail,icons_coins,order"
        const val BACKGROUND_FIELDS = "id,theme_id,name,image,coins"
        // Tab Icon cần is_unlocked (phần icon) + logos + backgrounds để dựng thẻ bộ icon.
        const val ICON_FIELDS = "id,name,thumbnail,is_unlocked,backgrounds,logos"
        const val PACK_ICONS = 6
        const val WHERE = ""
        const val ORDER_BY = "id desc"
        const val SUGGEST_ORDER_BY = "order asc,download_count desc"
        const val SUGGEST_THEME_FIELDS = "id,name,thumbnail,is_unlocked,order"
        const val SUGGEST_WALLPAPER_FIELDS = "id,name,thumbnail,backgrounds,order"
        const val SUGGEST_ICON_FIELDS = "id,name,thumbnail,is_unlocked,order,backgrounds,logos"
    }
}
