package com.ezla.oslauncher.themes.data.favorite

import android.content.Context
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Yêu thích chỉ lưu local (Room), chỉ gồm nền và bộ icon (không có theme). Mới thích lên đầu.
object FavoriteRepository {

    private const val TYPE_BACKGROUND = "background"
    private const val TYPE_ICONS = "icons"
    private const val PACK_ICONS = 6
    private const val ICON_SEPARATOR = "\n"

    private fun dao(context: Context) = FavoriteDatabase.get(context).dao()

    // Khoá của nền: mặc định -> "default"; có backgroundId -> id; dữ liệu mẫu (id 0) -> URL ảnh.
    fun wallpaperKey(isDefault: Boolean, backgroundId: Int, imageUrl: String): String = when {
        isDefault -> DefaultThemeProvider.DEFAULT_ID
        backgroundId > 0 -> backgroundId.toString()
        else -> imageUrl
    }

    suspend fun isWallpaperFavorite(context: Context, key: String): Boolean =
        withContext(Dispatchers.IO) { dao(context).exists(TYPE_BACKGROUND, key) }

    suspend fun isIconPackFavorite(context: Context, themeId: String): Boolean =
        withContext(Dispatchers.IO) { dao(context).exists(TYPE_ICONS, themeId) }

    // Đảo trạng thái, trả về trạng thái mới (true = đã thích).
    suspend fun toggleWallpaper(
        context: Context,
        key: String,
        name: String,
        imageUrl: String,
        backgroundId: Int,
        coins: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = dao(context)
        if (dao.exists(TYPE_BACKGROUND, key)) {
            dao.delete(TYPE_BACKGROUND, key)
            false
        } else {
            val image = imageUrl.takeUnless { key == DefaultThemeProvider.DEFAULT_ID }
            dao.upsert(FavoriteEntity(TYPE_BACKGROUND, key, name, image, backgroundId = backgroundId, coins = coins))
            true
        }
    }

    // Thẻ bộ icon chụp lại 6 icon đầu + nền đầu của theme tại thời điểm thả tim.
    suspend fun toggleIconPack(context: Context, detail: ThemeDetail): Boolean = withContext(Dispatchers.IO) {
        val dao = dao(context)
        if (dao.exists(TYPE_ICONS, detail.id)) {
            dao.delete(TYPE_ICONS, detail.id)
            false
        } else {
            val isDefault = DefaultThemeProvider.isDefault(detail.id)
            dao.upsert(
                FavoriteEntity(
                    type = TYPE_ICONS,
                    itemId = detail.id,
                    name = detail.name,
                    image = if (isDefault) null else detail.wallpapers.firstOrNull() ?: detail.previewUrl,
                    icons = if (isDefault) "" else detail.logos.take(PACK_ICONS)
                        .joinToString(ICON_SEPARATOR) { it.imageUrl }
                )
            )
            true
        }
    }

    // isUnlocked để false: MineFragment đối chiếu my-backgrounds rồi mới bật.
    suspend fun getWallpapers(context: Context): List<PreviewCard> = withContext(Dispatchers.IO) {
        dao(context).getByType(TYPE_BACKGROUND).mapNotNull { e ->
            if (e.itemId == DefaultThemeProvider.DEFAULT_ID) return@mapNotNull DefaultThemeProvider.wallpaper(context)
            val image = e.image ?: return@mapNotNull null
            PreviewCard(e.itemId, e.name, image, backgroundId = e.backgroundId, coins = e.coins)
        }
    }

    suspend fun getIconPacks(context: Context): List<IconPack> = withContext(Dispatchers.IO) {
        dao(context).getByType(TYPE_ICONS).map { e ->
            if (DefaultThemeProvider.isDefault(e.itemId)) return@map DefaultThemeProvider.iconPack(context)
            IconPack(
                id = e.itemId,
                name = e.name,
                icons = e.icons.split(ICON_SEPARATOR).filter { it.isNotBlank() },
                background = e.image
            )
        }
    }
}
