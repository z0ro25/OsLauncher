package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data

import android.graphics.drawable.Drawable

/**
 * 1 app đã cài hiển thị ở màn App Library.
 *  - [componentFlatten] : ComponentName.flattenToString() — khóa lưu assignment (khớp mẫu engine).
 *  - [packageName]      : package (dự phòng).
 *  - [label]            : tên app hiển thị.
 *  - [icon]             : icon app (nạp sẵn ở background).
 *  - [appCategory]      : ApplicationInfo.category (-1..7) — dùng SUY category mặc định khi engine
 *                         chưa có category gán tay (đồng bộ 2 chiều với grid).
 *  - [folderLabel]      : nhãn folder/category hiện tại — refresh ở onResume.
 */
data class InstalledAppItem(
    val componentFlatten: String,
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val appCategory: Int = -1,
    var folderLabel: String = "Other"
)
