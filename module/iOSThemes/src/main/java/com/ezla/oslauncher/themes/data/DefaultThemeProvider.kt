package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.IconPack
import android.content.Context
import com.ezla.oslauncher.themes.R
import com.ios.theme.def.R as DefR

// Theme mặc định = tài nguyên có sẵn của launcher trong :library:ThemeResource:ThemeDefault.
object DefaultThemeProvider {

    const val DEFAULT_ID = "default"

    fun theme(context: Context) = PreviewCard(
        DEFAULT_ID, context.getString(R.string.themes_default_name), R.drawable.themes_img_default_theme_thumb, true
    )

    fun wallpaper(context: Context) = PreviewCard(
        DEFAULT_ID, context.getString(R.string.themes_default_name), DefR.drawable.default_wallpaper, true
    )

    fun iconPack(context: Context) = IconPack(
        DEFAULT_ID,
        context.getString(R.string.themes_default_name),
        listOf(
            DefR.drawable.ic_app_phone, DefR.drawable.ic_app_mms, DefR.drawable.ic_app_camera,
            DefR.drawable.ic_app_setting, DefR.drawable.ic_app_clock, DefR.drawable.ic_app_calendar
        ),
        isDefault = true,
        background = DefR.drawable.default_wallpaper
    )
}
