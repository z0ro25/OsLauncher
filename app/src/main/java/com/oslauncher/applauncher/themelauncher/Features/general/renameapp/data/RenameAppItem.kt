package com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data

import android.graphics.drawable.Drawable

/**
 * 1 app đã cài dùng cho màn Change App Name.
 *  - [defaultLabel]: nhãn gốc từ hệ thống (LauncherApps info.label) — dùng để Reset.
 *  - [displayLabel]: nhãn đang hiển thị (custom nếu đã đặt, ngược lại = defaultLabel).
 */
data class RenameAppItem(
    val componentFlatten: String,
    val packageName: String,
    val defaultLabel: String,
    var displayLabel: String,
    var icon: Drawable?
)
