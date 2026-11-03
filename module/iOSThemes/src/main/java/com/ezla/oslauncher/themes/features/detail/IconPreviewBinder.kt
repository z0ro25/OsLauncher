package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeLogoItem
import com.ezla.oslauncher.themes.R
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
        fitDock(dock)
        // Gắn 1 lần cho mỗi dock (bind gọi lại khi đổi theme): đo xong / đổi cỡ thì co lại icon.
        if (dock.getTag(R.id.themes_tag_dock_fit) == null) {
            dock.setTag(R.id.themes_tag_dock_fit, true)
            dock.addOnLayoutChangeListener { v, l, _, r, _, ol, _, or, _ ->
                if (r - l != or - ol) v.post { fitDock(v as LinearLayout) }
            }
        }
    }

    // Icon cố định 56dp làm 4 ô dính sát khi khung preview hẹp: co icon theo bề ngang ô, chừa đúng
    // khe ngang như lưới (themes_install_icon_gap). Dock chưa đo (width 0) thì giữ cỡ tối đa.
    private fun fitDock(dock: LinearLayout) {
        val res = dock.resources
        val maxIcon = res.getDimensionPixelSize(R.dimen.themes_install_icon_size)
        val gap = res.getDimensionPixelSize(R.dimen.themes_install_icon_gap)
        val cell = (dock.width - dock.paddingLeft - dock.paddingRight) / DOCK_APPS.size
        val icon = if (cell > 0) minOf(maxIcon, cell - gap).coerceAtLeast(maxIcon / 2) else maxIcon
        for (i in 0 until dock.childCount) {
            val img = dock.getChildAt(i).findViewById<View>(R.id.imgAppIcon) ?: continue
            if (img.layoutParams.width == icon) continue
            img.layoutParams = img.layoutParams.apply {
                width = icon
                height = icon
            }
        }
    }

    // Khớp theo mã app của server hoặc label (vd "app_phone" / "phone").
    private fun matchesAny(logo: ThemeLogoItem, keys: Set<String>): Boolean =
        logo.packageName.trim().lowercase() in keys || logo.label.trim().lowercase() in keys
}
