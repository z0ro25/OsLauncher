package com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data

import android.graphics.drawable.Drawable

/**
 * 1 app đã cài dùng cho màn Change App Icon.
 *  - [displayLabel]: tên hiển thị (custom nếu đã đổi tên, ngược lại = nhãn gốc hệ thống).
 *  - [icon]: icon gốc hệ thống, chỉ để hiển thị trong list (KHÔNG phản ánh icon custom trong engine).
 */
data class ChangeIconItem(
    val componentFlatten: String,
    val packageName: String,
    val displayLabel: String,
    val icon: Drawable?
)
