package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import android.content.Context
import android.content.ContentResolver
import com.ezla.oslauncher.themes.R
import com.ios.theme.def.R as DefR

// Theme mặc định = tài nguyên có sẵn của launcher trong :library:ThemeResource:ThemeDefault.
object DefaultThemeProvider {

    const val DEFAULT_ID = "default"

    // Ảnh nền mặc định của launcher (dùng cả để hiển thị lẫn khôi phục nền).
    val WALLPAPER_RES = DefR.drawable.default_wallpaper

    // Bộ icon xem trước của theme mặc định (Figma Icon_SCS / App Icon): mã app_xxx + tên hiển thị.
    private val DEFAULT_ICONS = listOf(
        Triple("app_store", R.drawable.themes_default_icon_app_store, "App Store"),
        Triple("app_books", R.drawable.themes_default_icon_books, "Books"),
        Triple("app_calendar", R.drawable.themes_default_icon_calendar, "Calendar"),
        Triple("app_camera", R.drawable.themes_default_icon_camera, "Camera"),
        Triple("app_calculator", R.drawable.themes_default_icon_calculator, "Calculator"),
        Triple("app_clock", R.drawable.themes_default_icon_clock, "Clock"),
        Triple("app_contacts", R.drawable.themes_default_icon_contacts, "Contacts"),
        Triple("app_facetime", R.drawable.themes_default_icon_facetime, "FaceTime"),
        Triple("app_files", R.drawable.themes_default_icon_files, "Files"),
        Triple("app_find_my", R.drawable.themes_default_icon_find_my, "Find My"),
        Triple("app_fitness", R.drawable.themes_default_icon_fitness, "Fitness"),
        Triple("app_freeform", R.drawable.themes_default_icon_freeform, "Freeform"),
        Triple("app_games", R.drawable.themes_default_icon_games, "Games"),
        Triple("app_health", R.drawable.themes_default_icon_health, "Health"),
        Triple("app_home", R.drawable.themes_default_icon_home, "Home"),
        Triple("app_itunes_store", R.drawable.themes_default_icon_itunes_store, "iTunes Store"),
        Triple("app_keynote", R.drawable.themes_default_icon_keynote, "Keynote"),
        Triple("app_mail", R.drawable.themes_default_icon_mail, "Mail"),
        Triple("app_maps", R.drawable.themes_default_icon_maps, "Maps"),
        Triple("app_measure", R.drawable.themes_default_icon_measure, "Measure"),
        Triple("app_messages", R.drawable.themes_default_icon_messages, "Messages"),
        Triple("app_music", R.drawable.themes_default_icon_music, "Music"),
        Triple("app_news", R.drawable.themes_default_icon_news, "News"),
        Triple("app_notes", R.drawable.themes_default_icon_notes, "Notes"),
        Triple("app_numbers", R.drawable.themes_default_icon_numbers, "Numbers"),
        Triple("app_pages", R.drawable.themes_default_icon_pages, "Pages"),
        Triple("app_password", R.drawable.themes_default_icon_passwords, "Passwords"),
        Triple("app_phone", R.drawable.themes_default_icon_phone, "Phone"),
        Triple("app_photos", R.drawable.themes_default_icon_photos, "Photos"),
        Triple("app_podcasts", R.drawable.themes_default_icon_podcasts, "Podcasts"),
        Triple("app_preview", R.drawable.themes_default_icon_preview, "Preview"),
        Triple("app_reminders", R.drawable.themes_default_icon_reminders, "Reminders"),
        Triple("app_safari", R.drawable.themes_default_icon_safari, "Safari"),
        Triple("app_settings", R.drawable.themes_default_icon_settings, "Settings"),
        Triple("app_shortcuts", R.drawable.themes_default_icon_shortcuts, "Shortcuts"),
        Triple("app_stocks", R.drawable.themes_default_icon_stocks, "Stocks"),
        Triple("app_tips", R.drawable.themes_default_icon_tips, "Tips"),
        Triple("app_translate", R.drawable.themes_default_icon_translate, "Translate"),
        Triple("app_tv", R.drawable.themes_default_icon_tv, "TV"),
        Triple("app_voice_memos", R.drawable.themes_default_icon_voice_memos, "Voice Memos"),
        Triple("app_wallet", R.drawable.themes_default_icon_wallet, "Wallet"),
        Triple("app_watch", R.drawable.themes_default_icon_watch, "Watch"),
        Triple("app_weather", R.drawable.themes_default_icon_weather, "Weather"),
        Triple("app_airdrop", R.drawable.themes_default_icon_airdrop, "AirDrop")
    )

    // 6 icon hiện trên thẻ Default ở tab Mine > Icon.
    private val PACK_PREVIEW = listOf(
        "app_phone", "app_messages", "app_camera", "app_settings", "app_clock", "app_calendar"
    )

    fun isDefault(id: String?) = id == DEFAULT_ID

    // Thẻ theme mặc định ở tab Mine: ảnh chụp desktop mặc định (res/drawable/thumbnail_default.png).
    fun theme(context: Context) = PreviewCard(
        DEFAULT_ID, context.getString(R.string.themes_default_name), R.drawable.thumbnail_default, true
    )

    fun wallpaper(context: Context) = PreviewCard(
        DEFAULT_ID, context.getString(R.string.themes_default_name), DefR.drawable.default_wallpaper, true
    )

    fun iconPack(context: Context) = IconPack(
        DEFAULT_ID,
        context.getString(R.string.themes_default_name),
        PACK_PREVIEW.mapNotNull { code -> DEFAULT_ICONS.firstOrNull { it.first == code }?.second },
        isDefault = true,
        background = DefR.drawable.default_wallpaper
    )

    // Theme mặc định dạng ThemeDetail để đi chung các màn View/Install/Icon detail. Ảnh là URI
    // android.resource (Glide đọc như URL); luôn coi là đã mở khoá, Apply rẽ sang khôi phục mặc định.
    fun detail(context: Context) = ThemeDetail(
        id = DEFAULT_ID,
        name = context.getString(R.string.themes_default_name),
        previewUrl = resUri(context, R.drawable.thumbnail_default),
        iconCoins = 0,
        wallpaperCoins = 0,
        wallpapers = listOf(wallpaperUri(context)),
        logos = DEFAULT_ICONS.map { (code, res, label) -> ThemeLogoItem(code, resUri(context, res), label) },
        iconUnlocked = true,
        wallpaperUnlocked = true
    )

    fun wallpaperUri(context: Context) = resUri(context, WALLPAPER_RES)

    // Theme mặc định dựng tại chỗ, theme khác lấy từ API.
    suspend fun loadDetail(context: Context, repository: ThemeRepository, id: String): ThemeDetail? =
        if (isDefault(id)) detail(context) else repository.getThemeDetail(id)

    private fun resUri(context: Context, res: Int) =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$res"
}
