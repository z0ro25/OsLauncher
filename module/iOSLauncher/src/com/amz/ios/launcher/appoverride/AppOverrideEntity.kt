package com.amz.ios.launcher.appoverride

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Nguồn sự thật DUY NHẤT cho tên + logo custom của 1 app (theo yêu cầu model current/default).
 *
 * Mỗi app 1 dòng, khoá theo [componentFlatten] (ComponentName.flattenToString()).
 *  - [defaultName]/[defaultLogoPath]: giá trị GỐC hệ thống (defaultLogoPath luôn null = icon hệ thống,
 *    engine tự lấy icon gốc; giữ trường cho đúng model 2 default/2 current user yêu cầu).
 *  - [currentName]/[currentLogoPath]: giá trị ĐANG áp dụng. Đổi tên/logo CHỈ ghi vào current.
 *    currentLogoPath null = chưa đặt logo custom -> dùng icon hệ thống.
 *
 * Hiển thị (engine + app) LUÔN đọc current. Reset = đặt current về default (name) / null (logo).
 * DB riêng `app_override.db` nên KHÔNG bị luồng reload icon của engine xoá (khác bug ghi kép cũ).
 */
@Entity(tableName = "app_override")
data class AppOverrideEntity(
    @PrimaryKey val componentFlatten: String,
    val packageName: String,
    val defaultName: String,
    val currentName: String,
    val defaultLogoPath: String?,
    val currentLogoPath: String?
)
