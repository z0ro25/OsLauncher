package com.oslauncher.applauncher.themelauncher.theme.pack

import android.content.Context
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils

/**
 * Nơi ĐĂNG KÝ tất cả gói theme + nhớ theme đang chọn. Mục đích: sau này thêm tính năng đổi theme chỉ
 * cần thêm 1 [LauncherTheme] vào [THEMES], phần còn lại (chọn/nhớ/tra logo) không phải sửa.
 *
 * Hiện MỚI DỰNG CẤU TRÚC + 1 theme mặc định (structure only) — chưa có UI đổi theme, chưa nối cơ chế
 * áp theme vào launcher engine. current()/setCurrent() để sẵn cho bước sau.
 */
object ThemeRegistry {

    /** Key SharePref lưu id theme đang chọn. */
    const val KEY_SELECTED_THEME = "SELECTED_THEME_ID"

    /**
     * Theme MẶC ĐỊNH. appLogos điền theo TÊN drawable (logo nằm trong module/iOSLauncher/res/drawable*,
     * được merge vào app), theo mẫu:
     *   "com.facebook.katana" to "ic_app_facebook",
     *   "com.zing.zalo"       to "ic_app_zalo",
     * Package không có trong map -> dùng icon hệ thống. backgroundAsset trỏ 1 wallpaper trong assets;
     * đổi/null tuỳ theme.
     */
    val DEFAULT: LauncherTheme = LauncherTheme(
        id = "default",
        nameRes = R.string.theme_default_name,
        appLogos = emptyMap(),
        backgroundAsset = "wallpapers/wp1.png"
    )

    /** Thêm theme mới -> thêm vào danh sách này. */
    private val THEMES: List<LauncherTheme> = listOf(DEFAULT)

    /** Toàn bộ theme đã đăng ký. */
    fun all(): List<LauncherTheme> = THEMES

    /** Theme theo id; không khớp -> DEFAULT. */
    fun byId(id: String?): LauncherTheme = THEMES.firstOrNull { it.id == id } ?: DEFAULT

    /** Theme đang chọn (đọc từ SharePref); mặc định DEFAULT. */
    fun current(context: Context): LauncherTheme =
        byId(SharePrefUtils.getString(context, KEY_SELECTED_THEME, DEFAULT.id))

    /** Ghi nhớ theme đang chọn (chưa áp — dành cho bước nối UI/engine sau). */
    fun setCurrent(context: Context, id: String) {
        SharePrefUtils.putString(context, KEY_SELECTED_THEME, id)
    }
}
