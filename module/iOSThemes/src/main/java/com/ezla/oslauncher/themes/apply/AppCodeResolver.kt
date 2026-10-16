package com.ezla.oslauncher.themes.apply

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.truongnt.ios.launcher.provider.AppTypeProvider

// Ánh xạ mã app của server (logos[].package_name, vd "app_photos") sang app thật trên máy.
// Thứ tự: mã trùng app type của launcher (AppTypeProvider) -> alias tên khác -> package app bên
// thứ 3 -> coi như package Android thật. Không tìm được -> null (bỏ qua icon đó).
object AppCodeResolver {

    // Mã server khác tên với app type của launcher (AppTypeParser.APP_TYPE_*).
    private val TYPE_ALIASES = mapOf(
        "app_photos" to "app_gallery",
        "app_safari" to "app_browser",
        "app_messages" to "app_mms",
        "app_files" to "app_filemanager",
        "app_find" to "app_findmy",
        "app_voice" to "app_soundrecorder",
        "app_notes" to "app_note",
        "app_mail" to "app_gmail",
        "app_news" to "app_gnews",
        "app_tv" to "app_gtv",
        "app_wallet" to "app_gwallet",
        "app_facetime" to "app_gmeet",
        "app_books" to "app_gbooks",
        "app_appstore" to "app_market",
        "app_app_store" to "app_market",
        "app_number" to "app_numbers",
        "app_itunes" to "app_market"
    )

    // App bên thứ 3: thử lần lượt từng package, lấy package đầu tiên có trên máy.
    private val THIRD_PARTY = mapOf(
        "app_facebook" to listOf("com.facebook.katana"),
        "app_instagram" to listOf("com.instagram.android"),
        "app_tiktok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "app_youtube" to listOf("com.google.android.youtube"),
        "app_whatsapp" to listOf("com.whatsapp"),
        "app_x" to listOf("com.twitter.android"),
        "app_twitter" to listOf("com.twitter.android"),
        "app_linkedin" to listOf("com.linkedin.android"),
        "app_reddit" to listOf("com.reddit.frontpage"),
        "app_netflix" to listOf("com.netflix.mediaclient"),
        "app_spotify" to listOf("com.spotify.music"),
        "app_freeform" to listOf("com.google.android.apps.docs"),
        "app_stocks" to listOf("com.google.android.apps.finance")
    )

    // Logo của chính app Themes (server gửi package của module) -> shortcut "Themes" trên desktop;
    // đổi icon shortcut nội bộ nên chỉ đổi trong launcher, icon app ngoài hệ thống giữ nguyên.
    private const val THEMES_APP_CODE = "com.ezla.oslauncher.themes"
    private const val THEMES_SHORTCUT_CLASS = "com.truongnt.ios.launcher.shortcut.Theme"

    fun resolve(context: Context, code: String): ComponentName? {
        val key = code.trim().lowercase()
        if (key == THEMES_APP_CODE) return ComponentName(context.packageName, THEMES_SHORTCUT_CLASS)
        val pm = context.packageManager
        fromAppType(key)?.let { return it }
        TYPE_ALIASES[key]?.let { alias -> fromAppType(alias)?.let { return it } }
        THIRD_PARTY[key]?.forEach { pkg -> launchComponent(pm, pkg)?.let { return it } }
        // Server trả thẳng package Android (vd "com.whatsapp") thì dùng luôn.
        return if (key.contains('.')) launchComponent(pm, key) else null
    }

    // App type đã được launcher nạp (LauncherModel -> loadAppTypeFromDb), null nếu chưa nạp.
    private fun fromAppType(type: String): ComponentName? = try {
        AppTypeProvider.getAppTypeProvider()?.getComponentNameForAppType(type)
    } catch (t: Throwable) {
        null
    }

    private fun launchComponent(pm: PackageManager, packageName: String): ComponentName? = try {
        pm.getLaunchIntentForPackage(packageName)?.component
    } catch (t: Throwable) {
        null
    }
}
