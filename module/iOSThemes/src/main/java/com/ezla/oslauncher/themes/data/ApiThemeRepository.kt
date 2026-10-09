package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Api.ApiClient
import com.ezla.oslauncher.themes.Api.ApiLog
import com.ezla.oslauncher.themes.Models.ApiTheme
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R
import com.truongnt.ios.ioslite.common.CommonSdk
import kotlinx.coroutines.Dispatchers
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

    // Chi tiết 1 theme: dùng lại endpoint themes với where lọc id, xin thêm backgrounds+logos
    // (đã map sẵn ở ApiTheme). Lỗi -> null để màn Install báo thiếu dữ liệu.
    override suspend fun getThemeDetail(id: String): ThemeDetail? = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext() ?: return@withContext null
        runCatching {
            val response = ApiClient.api(ctx).getThemes(DETAIL_FIELDS, "id $id", "")
            if (!response.isSuccessful) error("theme detail failed: http=${response.code()}")
            response.body()?.data?.items?.firstOrNull()?.toDetail()
        }.getOrElse { error ->
            ApiLog.e("theme detail ✘ ${error.message}", error)
            null
        }
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
                    val img = theme.backgrounds.flatMap { it.image }.firstOrNull()?.url
                    val url = img?.full ?: img?.medium ?: return@mapNotNull null
                    PreviewCard(id = theme.id.toString(), name = theme.name, image = url)
                }
            }.getOrElse { error ->
                ApiLog.e("wallpapers ✘ ${error.message}", error)
                sample.getWallpapers(tab, categoryId)
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
        coins = coins,
        wallpapers = backgrounds.flatMap { it.image }
            .mapNotNull { it.url?.full ?: it.url?.medium },
        logos = logos.mapNotNull { logo ->
            val img = logo.image.firstOrNull()?.url
            val pkg = logo.packageName
            val url = img?.full ?: img?.medium
            if (pkg.isNullOrEmpty() || url.isNullOrEmpty()) null else ThemeLogoItem(pkg, url)
        }
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
        const val THEME_FIELDS = "id,category_id,name,thumbnail,order,coins"
        const val THEME_ORDER_BY = "order asc,download_count desc"
        // Chi tiết cần thêm backgrounds + logos (ảnh nền + icon theo package) + coins (giá mở).
        const val DETAIL_FIELDS = "id,name,thumbnail,coins,backgrounds,logos"
        // Tab Wallpapers cần backgrounds để lấy ảnh nền làm thẻ.
        const val WALLPAPER_FIELDS = "id,name,thumbnail,backgrounds,order"
    }
}
