package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import com.ezla.oslauncher.themes.databinding.ThemesItemInstallIconBinding

// Dựng preview desktop của 1 bộ icon (dùng chung màn Install và Icon detail): dock 4 ô cố định
// camera/phone/contacts/messages (thiếu thì để trống ô), icon còn lại vào lưới COLUMNS x ROWS vuốt ngang.
object IconPreviewBinder {

    const val COLUMNS = 4
    const val ROWS = 5

    private val DOCK_APPS = listOf(
        setOf("app_camera", "camera"),
        setOf("app_phone", "phone"),
        setOf("app_contacts", "app_contact", "contacts", "contact"),
        setOf("app_messages", "app_message", "app_mms", "messages", "message")
    )

    fun bind(logos: List<ThemeLogoItem>, pager: ViewPager2, dock: LinearLayout) {
        val dockLogos = DOCK_APPS.map { keys -> logos.firstOrNull { matchesAny(it, keys) } }
        val grid = (logos - dockLogos.filterNotNull().toSet()).chunked(COLUMNS * ROWS)
        pager.adapter = InstallIconPageAdapter(grid, COLUMNS, ROWS)

        dock.removeAllViews()
        dock.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(dock.context)
        for (logo in dockLogos) {
            val cell = ThemesItemInstallIconBinding.inflate(inflater, dock, false)
            cell.root.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            cell.root.setPadding(0, 0, 0, 0)
            cell.tvAppName.visibility = View.GONE
            if (logo != null) {
                Glide.with(cell.imgAppIcon).load(logo.imageUrl).into(cell.imgAppIcon)
            } else {
                cell.imgAppIcon.visibility = View.INVISIBLE
            }
            dock.addView(cell.root)
        }
    }

    // Khớp theo mã app của server hoặc label (vd "app_phone" / "phone").
    private fun matchesAny(logo: ThemeLogoItem, keys: Set<String>): Boolean =
        logo.packageName.trim().lowercase() in keys || logo.label.trim().lowercase() in keys
}
