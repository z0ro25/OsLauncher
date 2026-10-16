package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Api.ApiClient
import com.ezla.oslauncher.themes.Api.ApiLog
import com.ezla.oslauncher.themes.Models.ApiTheme
import com.ezla.oslauncher.themes.Models.IconSection
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R
import com.truongnt.ios.ioslite.common.CommonSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

// Nguồn dữ liệu thật: categories + themes lấy từ API qua SAG (tự ký + bootstrap device session).
// Lỗi mạng/HTTP -> rơi về dữ liệu mẫu để UI không trống, kèm log để đối chiếu.
class ApiThemeRepository : ThemeRepository {

    private val sample = SampleThemeRepository()

    override suspend fun getCategories(): List<ThemeCategory> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext()
        // Chưa có app context (CommonSdk chưa init) -> dùng dữ liệu mẫu thay vì crash.
        if (ctx == null) return@withContext sample.getCategories()
        runCatching {
            val response = ApiClient.api(ctx).getCategories(FIELDS, WHERE, ORDER_BY)
            if (!response.isSuccessful) error("categories failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty().map { category ->
                ThemeCategory(
                    id = category.id.toString(),
                    iconRes = iconFor(category.name),
                    title = category.name
                )
            }
        }.getOrElse { error ->
            // Lỗi mạng/HTTP: rơi về dữ liệu mẫu để chip không trống, có log để đối chiếu.
            ApiLog.e("categories ✘ ${error.message}", error)
            sample.getCategories()
        }
    }

    override suspend fun getThemes(tab: ThemeTab, categoryId: String): List<ThemeItem> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
            if (ctx == null) return@withContext sample.getThemes(tab, categoryId)
            runCatching {
                val response = ApiClient.api(ctx)
                    .getThemes(THEME_FIELDS, "category_id $categoryId", THEME_ORDER_BY)
                if (!response.isSuccessful) error("themes failed: http=${response.code()}")
                response.body()?.data?.items.orEmpty().map { theme ->
                    ThemeItem(id = theme.id.toString(), name = theme.name, previewUrl = previewUrl(theme))
                }
            }.getOrElse { error ->
                ApiLog.e("themes ✘ ${error.message}", error)
                sample.getThemes(tab, categoryId)
            }
        }

    // Chi tiết 1 theme qua endpoint themes. PHẢI chọn đúng phần tử theo id: nếu server bỏ qua where
    // thì trả cả danh sách, lấy firstOrNull sẽ ra nhầm theme khác. Lọc không ra -> thử lại không where.
    override suspend fun getThemeDetail(id: String): ThemeDetail? = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext null
        runCatching {
            findTheme(ctx, id, "id $id")
                ?: findTheme(ctx, id, WHERE).also {
                    ApiLog.e("theme detail: where id=$id không lọc được, lấy từ danh sách đầy đủ", null)
                }
        }.getOrElse { error ->
            ApiLog.e("theme detail ✘ ${error.message}", error)
            null
        }
    }

    private suspend fun findTheme(ctx: android.content.Context, id: String, where: String): ThemeDetail? {
        val response = ApiClient.api(ctx).getThemes(DETAIL_FIELDS, where, THEME_ORDER_BY)
        if (!response.isSuccessful) error("theme detail failed: http=${response.code()}")
        return response.body()?.data?.items?.firstOrNull { it.id.toString() == id }?.toDetail()
    }

    // Tab Wallpapers: mỗi theme đóng góp background đầu làm 1 thẻ wallpaper (ảnh FULL).
    override suspend fun getWallpapers(tab: ThemeTab, categoryId: String): List<PreviewCard> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
            if (ctx == null) return@withContext sample.getWallpapers(tab, categoryId)
            runCatching {
                val response = ApiClient.api(ctx)
                    .getThemes(WALLPAPER_FIELDS, "category_id $categoryId", THEME_ORDER_BY)
                if (!response.isSuccessful) error("wallpapers failed: http=${response.code()}")
                response.body()?.data?.items.orEmpty().mapNotNull { theme ->
                    val bg = theme.backgrounds.firstOrNull()
                    val img = theme.backgrounds.flatMap { it.image }.firstOrNull()?.url
                    val url = img?.full ?: img?.medium ?: return@mapNotNull null
                    PreviewCard(
                        id = theme.id.toString(), name = theme.name, image = url,
                        backgroundId = bg?.id ?: 0, coins = bg?.coins ?: 0,
                        isUnlocked = bg?.isUnlocked ?: false
                    )
                }
            }.getOrElse { error ->
                ApiLog.e("wallpapers ✘ ${error.message}", error)
                sample.getWallpapers(tab, categoryId)
            }
        }

    // Tab Icons: gọi /themes cho từng danh mục SONG SONG; danh mục lỗi/rỗng thì bỏ, không chặn
    // các section khác. API lỗi toàn bộ -> rơi về dữ liệu mẫu để màn không trống.
    override suspend fun getIconSections(categories: List<ThemeCategory>): List<IconSection> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
                ?: return@withContext sample.getIconSections(categories)
            val results = coroutineScope {
                categories.map { category ->
                    async { category to runCatching { iconCards(ctx, category.id) } }
                }.awaitAll()
            }
            if (results.isNotEmpty() && results.all { it.second.isFailure }) {
                ApiLog.e("icon sections ✘ ${results.first().second.exceptionOrNull()?.message}", null)
                return@withContext sample.getIconSections(categories)
            }
            results.mapNotNull { (category, result) ->
                result.exceptionOrNull()?.let { ApiLog.e("icon section ${category.id} ✘ ${it.message}", it) }
                result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { IconSection(category, it) }
            }
        }

    private suspend fun iconCards(ctx: android.content.Context, categoryId: String): List<IconThemeCard> {
        val response = ApiClient.api(ctx).getThemes(ICON_FIELDS, "category_id $categoryId", THEME_ORDER_BY)
        if (!response.isSuccessful) error("icon themes failed: http=${response.code()}")
        return response.body()?.data?.items.orEmpty().map { theme ->
            val bg = theme.backgrounds.flatMap { it.image }.firstOrNull()?.url
            IconThemeCard(
                id = theme.id.toString(),
                name = theme.name,
                background = bg?.medium ?: bg?.small ?: bg?.full,
                icons = theme.logos.sortedBy { it.order }.mapNotNull { logo ->
                    val url = logo.image.firstOrNull()?.url
                    url?.small ?: url?.medium ?: url?.full
                }.take(CARD_ICON_COUNT)
            )
        }
    }

    // Ảnh preview của theme = thumbnail đầu; ưu tiên medium/full cho vừa lưới.
    private fun previewUrl(theme: ApiTheme): String {
        val url = theme.thumbnail.firstOrNull()?.url ?: return ""
        return url.medium ?: url.full ?: url.small ?: url.extraSmall ?: ""
    }

    // Ánh xạ theme API -> chi tiết để áp dụng: nền lấy ảnh FULL, logo theo package_name.
    private fun ApiTheme.toDetail() = ThemeDetail(
        id = id.toString(),
        name = name,
        previewUrl = previewUrl(this),
        iconCoins = iconsCoins,
        wallpaperCoins = backgrounds.firstOrNull()?.coins ?: 0,
        wallpapers = backgrounds.flatMap { it.image }
            .mapNotNull { it.url?.full ?: it.url?.medium },
        logos = logos.mapNotNull { logo ->
            val img = logo.image.firstOrNull()?.url
            val pkg = logo.packageName
            val url = img?.full ?: img?.medium
            if (pkg.isNullOrEmpty() || url.isNullOrEmpty()) null else ThemeLogoItem(pkg, url, logo.label.orEmpty())
        },
        backgroundId = backgrounds.firstOrNull()?.id ?: 0,
        iconUnlocked = isUnlocked,
        wallpaperUnlocked = backgrounds.firstOrNull()?.isUnlocked ?: false
    )

    // Chip dùng icon local (API không trả icon): map theo tên, không khớp -> icon mặc định.
    private fun iconFor(name: String): Int {
        val n = name.lowercase()
        return when {
            n.contains("os27") || n.contains("os 27") -> R.drawable.themes_ic_chip_os27
            n.contains("black") -> R.drawable.themes_ic_chip_black
            n.contains("cartoon") -> R.drawable.themes_ic_chip_cartoon
            n.contains("neon") -> R.drawable.themes_ic_chip_neon
            n.contains("cute") -> R.drawable.themes_ic_chip_cute
            n.contains("sport") -> R.drawable.themes_ic_chip_sport
            else -> R.drawable.themes_ic_chip_new
        }
    }

    private companion object {
        // Tham số theo curl của server; đổi ở đây nếu server yêu cầu khác.
        const val FIELDS = "id,name,thumbnail,order"
        const val WHERE = ""
        const val ORDER_BY = "order asc,id desc"
        const val THEME_FIELDS = "id,category_id,name,thumbnail,order,icons_coins"
        const val THEME_ORDER_BY = "order asc,download_count desc"
        // Chi tiết cần backgrounds + logos (ảnh nền + icon theo mã app) + icons_coins (giá mở icon).
        const val DETAIL_FIELDS = "id,name,thumbnail,icons_coins,is_unlocked,backgrounds,logos"
        // Tab Wallpapers cần backgrounds để lấy ảnh nền làm thẻ.
        // backgrounds mang id + coins + is_unlocked để màn wallpaper detail gọi /unlocks.
        const val WALLPAPER_FIELDS = "id,name,thumbnail,backgrounds,order"
        // Tab Icons: thẻ cần ảnh nền (backgrounds) + 6 logo đầu (logos).
        const val ICON_FIELDS = "id,category_id,name,thumbnail,order,backgrounds,logos"
        const val CARD_ICON_COUNT = 6
    }
}
