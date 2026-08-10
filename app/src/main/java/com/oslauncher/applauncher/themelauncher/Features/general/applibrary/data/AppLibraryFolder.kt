package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data

/**
 * 1 folder tùy chỉnh do user tạo ở màn Manage Library.
 *  - [id]    : UUID bất biến, dùng làm targetId khi gán app -> folder.
 *  - [name]  : tên hiển thị (<= 30 ký tự).
 *  - [order] : thứ tự hiển thị/kéo sắp xếp.
 */
data class AppLibraryFolder(
    val id: String,
    var name: String,
    var order: Int
)
