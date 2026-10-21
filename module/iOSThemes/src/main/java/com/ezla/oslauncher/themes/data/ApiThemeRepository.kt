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
import com.ezla.oslauncher.themes.Models.ThemeBackground
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.Models.WallpaperPage
import com.ezla.oslauncher.themes.test.FileLogger
import com.google.gson.Gson
import com.truongnt.ios.ioslite.common.CommonSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response

// Nguồn dữ liệu thật: categories + themes lấy từ API qua SAG (tự ký + bootstrap device session).
// Lỗi mạng/HTTP -> rơi về dữ liệu mẫu để UI không trống, kèm log để đối chiếu.
class ApiThemeRepository : ThemeRepository {

    private val sample = SampleThemeRepository()
    private val gson = Gson()

    override suspend fun getCategories(): List<ThemeCategory> = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext()
        // Chưa có app context (CommonSdk chưa init) -> dùng dữ liệu mẫu thay vì crash.
        if (ctx == null) {
            logNoContext("categories")
            return@withContext sample.getCategories()
        }
        logStart(ctx, "categories")
        runCatching {
            val response = logCall("categories", "fields=$FIELDS where=$WHERE order_by=$ORDER_BY") {
                ApiClient.api(ctx).getCategories(FIELDS, WHERE, ORDER_BY)
            }
            if (!response.isSuccessful) error("categories failed: http=${response.code()}")
            response.body()?.data?.items.orEmpty().map { category ->
                val url = category.thumbnail.firstOrNull()?.url
                ThemeCategory(
                    id = category.id.toString(),
                    iconUrl = url?.small ?: url?.extraSmall ?: url?.medium ?: url?.full,
                    title = category.name
                )
            }.also { FileLogger.d(TAG, "categories items=${it.size}") }
        }.getOrElse { error ->
            // Lỗi mạng/HTTP: rơi về dữ liệu mẫu để chip không trống, có log để đối chiếu.
            ApiLog.e("categories ✘ ${error.message}", error)
            FileLogger.e(TAG, "categories ✘ ${error.message}", error)
//            sample.getCategories()
            listOf()
        }
    }

    override suspend fun getThemes(tab: ThemeTab, categoryId: String): List<ThemeItem> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
            if (ctx == null) {
                logNoContext("themes")
                return@withContext sample.getThemes(tab, categoryId)
            }
            logStart(ctx, "themes tab=$tab category=$categoryId")
            runCatching {
                val where = "category_id $categoryId"
                val response = logCall("themes", "tab=$tab fields=$THEME_FIELDS where=$where order_by=$THEME_ORDER_BY") {
                    ApiClient.api(ctx).getThemes(THEME_FIELDS, where, THEME_ORDER_BY)
                }
                if (!response.isSuccessful) error("themes failed: http=${response.code()}")
                response.body()?.data?.items.orEmpty().map { theme ->
                    ThemeItem(
                        id = theme.id.toString(), name = theme.name, previewUrl = previewUrl(theme),
                        isUnlocked = theme.isUnlocked
                    )
                }.also { FileLogger.d(TAG, "themes category=$categoryId items=${it.size}") }
            }.getOrElse { error ->
                ApiLog.e("themes ✘ ${error.message}", error)
                FileLogger.e(TAG, "themes category=$categoryId ✘ ${error.message}", error)
//                sample.getThemes(tab, categoryId)
                listOf()
            }
        }

    // Chi tiết 1 theme qua endpoint themes. PHẢI chọn đúng phần tử theo id: nếu server bỏ qua where
    // thì trả cả danh sách, lấy firstOrNull sẽ ra nhầm theme khác. Lọc không ra -> thử lại không where.
    override suspend fun getThemeDetail(id: String): ThemeDetail? = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext()
        if (ctx == null) {
            logNoContext("theme detail")
            return@withContext null
        }
        logStart(ctx, "theme detail id=$id")
        runCatching {
            findTheme(ctx, id, "id $id")
                ?: findTheme(ctx, id, WHERE).also {
                    ApiLog.e("theme detail: where id=$id không lọc được, lấy từ danh sách đầy đủ", null)
                    FileLogger.w(TAG, "theme detail id=$id: where không lọc được, found=${it != null}")
                }
        }.getOrElse { error ->
            ApiLog.e("theme detail ✘ ${error.message}", error)
            FileLogger.e(TAG, "theme detail id=$id ✘ ${error.message}", error)
            null
        }
    }

    private suspend fun findTheme(ctx: android.content.Context, id: String, where: String): ThemeDetail? {
        val response = logCall("theme detail", "id=$id fields=$DETAIL_FIELDS where='$where' order_by=$THEME_ORDER_BY") {
            ApiClient.api(ctx).getThemes(DETAIL_FIELDS, where, THEME_ORDER_BY)
        }
        if (!response.isSuccessful) error("theme detail failed: http=${response.code()}")
        return response.body()?.data?.items?.firstOrNull { it.id.toString() == id }?.toDetail()
    }

    // /backgrounds chỉ lọc được theo theme_id -> lấy theme của danh mục rồi gọi song song từng theme,
    // giữ thứ tự theme như /themes. Theme lỗi thì bỏ; lỗi bước lấy theme -> dữ liệu mẫu.
    override suspend fun getWallpapers(tab: ThemeTab, categoryId: String): List<PreviewCard> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
            if (ctx == null) {
                logNoContext("wallpapers")
                return@withContext sample.getWallpapers(tab, categoryId)
            }
            logStart(ctx, "wallpapers tab=$tab category=$categoryId")
            runCatching {
                val where = "category_id $categoryId"
                val response = logCall(
                    "wallpaper themes", "tab=$tab fields=$CATEGORY_THEME_FIELDS where=$where order_by=$THEME_ORDER_BY"
                ) {
                    ApiClient.api(ctx).getThemes(CATEGORY_THEME_FIELDS, where, THEME_ORDER_BY)
                }
                if (!response.isSuccessful) error("wallpaper themes failed: http=${response.code()}")
                val themes = response.body()?.data?.items.orEmpty()
                coroutineScope {
                    themes.map { theme ->
                        async {
                            runCatching { themeBackgrounds(ctx, theme.id) }
                                .onFailure {
                                    ApiLog.e("backgrounds theme ${theme.id} ✘ ${it.message}", it)
                                    FileLogger.e(TAG, "backgrounds theme ${theme.id} ✘ ${it.message}", it)
                                }
                                .getOrDefault(emptyList())
                                .flatMap { bg -> bg.toCards(theme.name) }
                        }
                    }.awaitAll().flatten()
                }.also { FileLogger.d(TAG, "wallpapers category=$categoryId themes=${themes.size} cards=${it.size}") }
            }.getOrElse { error ->
                ApiLog.e("wallpapers ✘ ${error.message}", error)
                FileLogger.e(TAG, "wallpapers category=$categoryId ✘ ${error.message}", error)
//                sample.getWallpapers(tab, categoryId)
                listOf()
            }
        }

    // Trang 1 lỗi -> dữ liệu mẫu để lưới không trống; trang sau lỗi -> null để màn cho thử lại.
    override suspend fun getAllWallpapers(page: Int): WallpaperPage? = withContext(Dispatchers.IO) {
        val ctx = CommonSdk.getApplicationContext()
        if (ctx == null) {
            logNoContext("backgrounds")
            return@withContext sample.getAllWallpapers(page)
        }
        logStart(ctx, "backgrounds page=$page")
        runCatching {
            val response = logCall(
                "backgrounds",
                "fields=$BACKGROUND_FIELDS where=$WHERE order_by=$BACKGROUND_ORDER_BY limit=$PAGE_SIZE page=$page"
            ) {
                ApiClient.api(ctx).getBackgrounds(BACKGROUND_FIELDS, WHERE, BACKGROUND_ORDER_BY, PAGE_SIZE, page)
            }
            if (!response.isSuccessful) error("backgrounds failed: http=${response.code()}")
            val data = response.body()?.data
            WallpaperPage(
                cards = data?.items.orEmpty().flatMap { it.toCards(it.name.orEmpty()) },
                hasMore = !data?.nextPageUrl.isNullOrEmpty()
            ).also { FileLogger.d(TAG, "backgrounds page=$page cards=${it.cards.size} hasMore=${it.hasMore}") }
        }.getOrElse { error ->
            ApiLog.e("backgrounds page $page ✘ ${error.message}", error)
            FileLogger.e(TAG, "backgrounds page=$page ✘ ${error.message}", error)
            null
        }
    }

    // Mọi nền của 1 theme (thường 1, lấy tối đa BACKGROUNDS_PER_THEME).
    private suspend fun themeBackgrounds(ctx: android.content.Context, themeId: Int): List<ThemeBackground> {
        val where = "theme_id $themeId"
        val response = logCall(
            "backgrounds theme",
            "fields=$BACKGROUND_FIELDS where=$where order_by=$BACKGROUND_ORDER_BY limit=$BACKGROUNDS_PER_THEME page=1"
        ) {
            ApiClient.api(ctx).getBackgrounds(BACKGROUND_FIELDS, where, BACKGROUND_ORDER_BY, BACKGROUNDS_PER_THEME, 1)
        }
        if (!response.isSuccessful) error("http=${response.code()}")
        return response.body()?.data?.items.orEmpty()
    }

    // Mỗi ảnh của background = 1 thẻ; mang id/giá/is_unlocked của background để detail gọi /unlocks.
    private fun ThemeBackground.toCards(name: String): List<PreviewCard> = image.mapNotNull { img ->
        val url = img.url?.full ?: img.url?.medium ?: return@mapNotNull null
        PreviewCard(
            id = themeId.toString(), name = name, image = url,
            backgroundId = id, coins = coins, isUnlocked = isUnlocked
        )
    }

    // Tab Icons: gọi /themes cho từng danh mục SONG SONG; danh mục lỗi/rỗng thì bỏ, không chặn
    // các section khác. API lỗi toàn bộ -> rơi về dữ liệu mẫu để màn không trống.
    override suspend fun getIconSections(categories: List<ThemeCategory>): List<IconSection> =
        withContext(Dispatchers.IO) {
            val ctx = CommonSdk.getApplicationContext()
            if (ctx == null) {
                logNoContext("icon sections")
                return@withContext sample.getIconSections(categories)
            }
            logStart(ctx, "icon sections categories=${categories.map { it.id }}")
            val results = coroutineScope {
                categories.map { category ->
                    async { category to runCatching { iconCards(ctx, category.id) } }
                }.awaitAll()
            }
            if (results.isNotEmpty() && results.all { it.second.isFailure }) {
                ApiLog.e("icon sections ✘ ${results.first().second.exceptionOrNull()?.message}", null)
                FileLogger.e(TAG, "icon sections ✘ tất cả lỗi -> dữ liệu mẫu", results.first().second.exceptionOrNull())
                return@withContext sample.getIconSections(categories)
            }
            results.mapNotNull { (category, result) ->
                result.exceptionOrNull()?.let {
                    ApiLog.e("icon section ${category.id} ✘ ${it.message}", it)
                    FileLogger.e(TAG, "icon section ${category.id} ✘ ${it.message}", it)
                }
                result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { IconSection(category, it) }
            }
        }

    private suspend fun iconCards(ctx: android.content.Context, categoryId: String): List<IconThemeCard> {
        val where = "category_id $categoryId"
        val response = logCall("icon themes", "fields=$ICON_FIELDS where=$where order_by=$THEME_ORDER_BY") {
            ApiClient.api(ctx).getThemes(ICON_FIELDS, where, THEME_ORDER_BY)
        }
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
                }.take(CARD_ICON_COUNT),
                isUnlocked = theme.isUnlocked
            )
        }
    }

    // CommonSdk chưa có context thì FileLogger chưa init được -> dòng này có thể chỉ ra Logcat.
    private fun logNoContext(name: String) =
        FileLogger.e(TAG, "$name: CommonSdk chưa có context -> dữ liệu mẫu/null")

    // Init trước khi log: lần gọi đầu ApiClient chưa build nên FileLogger chưa có context.
    private fun logStart(ctx: android.content.Context, name: String) {
        FileLogger.init(ctx)
        FileLogger.d(TAG, "$name: start")
    }

    // Ghi tham số đầu vào, request thật (url + header), response (code + header + body). Exception
    // (mạng/SAG) không có response -> ghi stacktrace rồi ném lại để luồng xử lý lỗi cũ giữ nguyên.
    private suspend fun <T> logCall(name: String, input: String, call: suspend () -> Response<T>): Response<T> {
        FileLogger.d(TAG, "$name → input: $input")
        val response = try {
            call()
        } catch (e: Throwable) {
            FileLogger.e(TAG, "$name ✘ exception, không có response: ${e.message}", e)
            throw e
        }
        val raw = response.raw()
        val req = raw.request
        // Chỉ đọc errorBody khi lỗi: nhánh lỗi không dùng body nữa nên đọc hết không ảnh hưởng.
        val body = if (response.isSuccessful) runCatching { gson.toJson(response.body()) }.getOrElse { "<toJson lỗi: ${it.message}>" }
        else runCatching { response.errorBody()?.string() }.getOrElse { "<đọc errorBody lỗi: ${it.message}>" }
        val detail = buildString {
            append("$name ${if (response.isSuccessful) "✔" else "✘"} ")
            append("${raw.receivedResponseAtMillis - raw.sentRequestAtMillis}ms\n")
            append("REQUEST ${req.method} ${req.url}\n").append(headersText(req.headers))
            append("RESPONSE ${raw.protocol} ${response.code()} ${response.message()}\n").append(headersText(raw.headers))
            append("BODY ").append(body)
        }
        if (response.isSuccessful) FileLogger.d(TAG, detail) else FileLogger.e(TAG, detail)
        return response
    }

    // File nằm ở Download (app khác đọc được) -> che token/chữ ký, chỉ giữ 8 ký tự đầu để đối chiếu.
    private fun headersText(headers: okhttp3.Headers): String = buildString {
        headers.forEach { (key, value) ->
            val secret = key.equals("Authorization", true) || key.contains("signature", true)
            append("  $key: ${if (secret) value.take(8) + "***" else value}\n")
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

    private companion object {
        const val TAG = "ApiThemeRepository"
        // Tham số theo curl của server; đổi ở đây nếu server yêu cầu khác.
        const val FIELDS = "id,name,thumbnail,order"
        const val WHERE = ""
        const val ORDER_BY = "order asc,id desc"
        // is_unlocked: ẩn/hiện nhãn New trên thẻ theme.
        const val THEME_FIELDS = "id,category_id,name,thumbnail,order,icons_coins,is_unlocked"
        const val THEME_ORDER_BY = "order asc,download_count desc"
        // Chi tiết cần backgrounds + logos (ảnh nền + icon theo mã app) + icons_coins (giá mở icon).
        const val DETAIL_FIELDS = "id,name,thumbnail,icons_coins,is_unlocked,backgrounds,logos"
        // Tab Wallpapers: /backgrounds theo curl của server (+ is_unlocked); theme chỉ cần id + name.
        const val BACKGROUND_FIELDS = "id,theme_id,name,image,coins,is_unlocked"
        const val BACKGROUND_ORDER_BY = "id desc"
        const val CATEGORY_THEME_FIELDS = "id,name,order"
        const val PAGE_SIZE = 15
        const val BACKGROUNDS_PER_THEME = 50
        // Tab Icons: thẻ cần ảnh nền (backgrounds) + 6 logo đầu (logos).
        const val ICON_FIELDS = "id,category_id,name,thumbnail,order,backgrounds,logos,is_unlocked"
        const val CARD_ICON_COUNT = 6
    }
}
